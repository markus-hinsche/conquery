package com.bakdata.conquery.resources.hierarchies;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AdminPermission;
import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import io.swagger.annotations.Api;

/**
 * This class ensures that all users have the admin permission in order to
 * access admin resources.
 */
@Api(tags = "admin")
public abstract class HAdmin extends HAuthorized implements Authorized {

	@Inject
	protected AdminProcessor processor;
	
	@Override
	@PostConstruct
	public void init() {
		super.init();

		user.authorize(this, Ability.READ);
	}

	@Override
	public ConqueryPermission createPermission(Set<Ability> abilities) {
		return AdminPermission.onDomain(abilities);
	}
}
