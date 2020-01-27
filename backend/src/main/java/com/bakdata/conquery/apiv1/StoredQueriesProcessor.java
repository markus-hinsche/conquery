package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.addPermission;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.removePermission;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Namespaces;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StoredQueriesProcessor {

	@Getter
	private final Namespaces namespaces;
	private final MasterMetaStorage storage;
	private final AuthorizationStorage authStorage;

	public StoredQueriesProcessor(Namespaces namespaces, AuthorizationStorage authStorage) {
		this.namespaces = namespaces;
		this.storage = namespaces.getMetaStorage();
		this.authStorage =authStorage;
	}

	public Stream<ExecutionStatus> getAllQueries(Dataset dataset, HttpServletRequest req, User user) {
		Collection<ManagedExecution> allQueries = storage.getAllExecutions();

		return allQueries
			.stream()
			// to exclude subtypes from somewhere else
			.filter(q -> (q instanceof ManagedQuery) && ((ManagedQuery) q).getQuery().getClass().equals(ConceptQuery.class))
			.filter(q -> q.getDataset().equals(dataset.getId()))
			.filter(q -> user.isPermitted(QueryPermission.onInstance(Ability.READ, q.getId())))
			.flatMap(mq -> {
				try {
					return Stream.of(
						mq.buildStatus(
							URLBuilder.fromRequest(req),
							user));
				}
				catch (Exception e) {
					log.warn("Could not build status of " + mq, e);
					return Stream.empty();
				}
			});
	}

	public void deleteQuery(Dataset dataset, ManagedExecution query) {
		storage.removeExecution(query.getId());
	}

	/**
	 * (Un)Shares a query with all groups a user is in.
	 */
	public void shareWithAllGroups(User user, ManagedQuery query, boolean shared) throws JSONException {
		List<Group> userGroups = storage.getAllGroups().stream().filter(group -> group.containsMember(user)).collect(Collectors.toList());
		for (Group group : userGroups) {
			shareWithGroup(user, query, group, shared);
		}
	}

	/**
	 * (Un)Shares a query with a specific groups.
	 */
	public void shareWithGroups(User user, ManagedQuery query, Collection<GroupId> shareGroups, boolean shared) throws JSONException {
		for (GroupId groupId : shareGroups) {
			Group group = storage.getGroup(groupId);
			try {
				shareWithGroup(user, query, group, shared);
			}
			catch (IllegalArgumentException e) {
				// Log unsuccessful shares
				log.warn("Could not {} query. Cause: {}", shared ? "share" : "unshare", e.getMessage());
			}
		}
	}

	/**
	 * (Un)Shares a query with a specific group.
	 */
	public void shareWithGroup(User user, ManagedQuery query, Group shareGroup, boolean shared) throws JSONException {
		updateQueryVersions(user, query, Ability.SHARE, q -> {
			ConqueryPermission queryPermission = QueryPermission.onInstance(AbilitySets.QUERY_EXECUTOR, q.getId());
			List<Group> userGroups = storage
				.getAllGroups()
				.stream()
				.filter(group -> group.containsMember(user))
				.collect(Collectors.toList());
			if (!userGroups.contains(shareGroup)) {
				throw new IllegalArgumentException(
					String
						.format(
							"User %s tried to (un)share query %s to group %s, which it does not belong to. Belongs to: %",
							user.getId(),
							q.getId(),
							shareGroup.getId(),
							userGroups));
			}
			try {
				if (shared) {
					addPermission(shareGroup, queryPermission, authStorage);
					log.trace("User {} shares query {}. Adding permission {} to group {}.", user, q.getId(), queryPermission, shareGroup);
				}
				else {
					removePermission(shareGroup, queryPermission, authStorage);
					log
						.trace(
							"User {} unshares query {}. Removing permission {} from group {}.",
							user,
							q.getId(),
							queryPermission,
							shareGroup);
				}
				q.setShared(shared);
			}
			catch (JSONException e) {
				log.error("Failed to set shared status for query " + query, e);
			}

		});
	}

	public void updateQueryLabel(User user, ManagedQuery query, String label) throws JSONException {
		updateQueryVersions(user, query, Ability.LABEL, q -> q.setLabel(label));
	}

	public void tagQuery(User user, ManagedQuery query, String[] newTags) throws JSONException {
		updateQueryVersions(user, query, Ability.TAG, q -> q.setTags(newTags));
	}

	public void updateQueryVersions(User user, ManagedQuery query, Ability requiredAbility, Consumer<ManagedQuery> updater) throws JSONException {
		authorize(user, query, requiredAbility);

		for (Namespace ns : namespaces.getNamespaces()) {

			if (user.isPermitted(DatasetPermission.onInstance(Ability.READ.asSet(), ns.getDataset().getId()))) {
				ManagedExecutionId id = new ManagedExecutionId(ns.getDataset().getId(), query.getQueryId());
				ManagedQuery exec = (ManagedQuery) storage.getExecution(id);
				if (exec != null) {
					if (user.isPermitted(QueryPermission.onInstance(requiredAbility, id))) {
						updater.accept(exec);
						storage.updateExecution(exec);
					}
				}
			}
		}
	}

	public ExecutionStatus getQueryWithSource(Dataset dataset, ManagedExecutionId queryId, User user) {
		ManagedExecution query = storage.getExecution(queryId);
		if (query == null) {
			return null;
		}
		return query.buildStatus(user);
	}

	public void shareQuery(User user, ManagedQuery query, Collection<GroupId> groupIds, Boolean shared) throws JSONException {
		if (groupIds == null) {
			// If no specific group is given (un)share with all groups
			shareWithAllGroups(user, query, shared);
		}
		else {
			shareWithGroups(user, query, groupIds, shared);
		}
	}

}
