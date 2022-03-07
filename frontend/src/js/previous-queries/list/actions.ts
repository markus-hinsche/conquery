import { StateT } from "app-types";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { ActionType, createAction, createAsyncAction } from "typesafe-actions";

import {
  useGetQueries,
  usePatchQuery,
  useGetQuery,
  useDeleteQuery,
  usePatchFormConfig,
  useDeleteFormConfig,
} from "../../api/api";
import {
  DatasetIdT,
  GetQueriesResponseT,
  GetQueryResponseT,
  QueryIdT,
} from "../../api/types";
import { ErrorObject, errorPayload } from "../../common/actions";
import { useDatasetId } from "../../dataset/selectors";
import {
  deleteFormConfigSuccess,
  patchFormConfigSuccess,
} from "../../external-forms/form-configs/actions";
import { setMessage } from "../../snack-message/actions";

import { PreviousQueryIdT } from "./reducer";

export type PreviousQueryListActions = ActionType<
  | typeof loadQueries
  | typeof loadQuery
  | typeof renameQuery
  | typeof retagQuery
  | typeof shareQuerySuccess
  | typeof deleteQuerySuccess
  | typeof addFolder
  | typeof removeFolder
>;

export const loadQueries = createAsyncAction(
  "queries/LOAD_QUERIES_START",
  "queries/LOAD_QUERIES_SUCCESS",
  "queries/LOAD_QUERIES_ERROR",
)<undefined, { data: GetQueriesResponseT }, ErrorObject>();

export const useLoadQueries = () => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const getQueries = useGetQueries();

  return async (datasetId: DatasetIdT) => {
    dispatch(loadQueries.request());

    try {
      const data = await getQueries(datasetId);

      return dispatch(loadQueries.success({ data }));
    } catch (e) {
      dispatch(setMessage({ message: t("previousQueries.error") }));

      return dispatch(loadQueries.failure(errorPayload(e as Error, {})));
    }
  };
};

interface QueryContext {
  queryId: QueryIdT;
}
export const loadQuery = createAsyncAction(
  "queries/LOAD_QUERY_START",
  "queries/LOAD_QUERY_SUCCESS",
  "queries/LOAD_QUERY_ERROR",
)<
  QueryContext,
  QueryContext & { data: GetQueryResponseT },
  QueryContext & ErrorObject
>();

export const useLoadQuery = () => {
  const dispatch = useDispatch();
  const getQuery = useGetQuery();
  const { t } = useTranslation();

  return (datasetId: DatasetIdT, queryId: PreviousQueryIdT) => {
    dispatch(loadQuery.request({ queryId }));

    return getQuery(datasetId, queryId).then(
      (r) => dispatch(loadQuery.success({ queryId, data: r })),
      () =>
        dispatch(
          loadQuery.failure({
            queryId,
            message: t("previousQuery.loadError"),
          }),
        ),
    );
  };
};

export const renameQuery = createAsyncAction(
  "queries/RENAME_QUERY_START",
  "queries/RENAME_QUERY_SUCCESS",
  "queries/RENAME_QUERY_ERROR",
)<QueryContext, QueryContext & { label: string }, QueryContext & ErrorObject>();

export const useRenameQuery = () => {
  const dispatch = useDispatch();
  const patchQuery = usePatchQuery();
  const { t } = useTranslation();

  return (datasetId: DatasetIdT, queryId: PreviousQueryIdT, label: string) => {
    dispatch(renameQuery.request({ queryId }));

    return patchQuery(datasetId, queryId, { label }).then(
      () => dispatch(renameQuery.success({ queryId, label })),
      () =>
        dispatch(
          renameQuery.failure({
            queryId,
            message: t("previousQuery.renameError"),
          }),
        ),
    );
  };
};

export const retagQuery = createAsyncAction(
  "queries/RETAG_QUERY_START",
  "queries/RETAG_QUERY_SUCCESS",
  "queries/RETAG_QUERY_ERROR",
)<
  QueryContext,
  QueryContext & { tags: string[] },
  QueryContext & ErrorObject
>();

export const useRetagQuery = () => {
  const dispatch = useDispatch();
  const patchQuery = usePatchQuery();
  const datasetId = useDatasetId();
  const { t } = useTranslation();

  return (queryId: PreviousQueryIdT, tags: string[]) => {
    if (!datasetId) {
      return Promise.resolve();
    }

    dispatch(retagQuery.request({ queryId }));

    return patchQuery(datasetId, queryId, { tags }).then(
      () => {
        dispatch(retagQuery.success({ queryId, tags }));
      },
      () =>
        dispatch(
          retagQuery.failure({
            queryId,
            message: t("previousQuery.retagError"),
          }),
        ),
    );
  };
};

export const addFolder = createAction("queries/ADD_FOLDER")<{
  folderName: string;
}>();
export const removeFolder = createAction("queries/REMOVE_FOLDER")<{
  folderName: string;
}>();

export const shareQuerySuccess = createAction(
  "queries/TOGGLE_SHARE_QUERY_SUCCESS",
)<{
  queryId: string;
  groups: PreviousQueryIdT[];
}>();

export const deleteQuerySuccess = createAction("queries/DELETE_QUERY_SUCCESS")<{
  queryId: string;
}>();

export const useRemoveQuery = (
  queryId: PreviousQueryIdT,
  onSuccess?: () => void,
) => {
  const { t } = useTranslation();
  const datasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId,
  );
  const dispatch = useDispatch();
  const deleteQuery = useDeleteQuery();

  return async () => {
    if (!datasetId) return;

    try {
      await deleteQuery(datasetId, queryId);

      dispatch(deleteQuerySuccess({ queryId }));

      if (onSuccess) {
        onSuccess();
      }
    } catch (e) {
      dispatch(setMessage({ message: t("previousQuery.deleteError") }));
    }
  };
};

export const useUpdateFormConfig = () => {
  const datasetId = useDatasetId();
  const dispatch = useDispatch();
  const patchFormConfig = usePatchFormConfig();

  const [loading, setLoading] = useState(false);

  const updateFormConfig = async (
    configId: string,
    attributes: {
      shared?: boolean;
      label?: string;
      tags?: string[];
    },
    errorMessage: string,
  ) => {
    if (!datasetId) return;

    setLoading(true);
    try {
      await patchFormConfig(datasetId, configId, attributes);

      dispatch(patchFormConfigSuccess(configId, attributes));
    } catch (e) {
      dispatch(setMessage({ message: errorMessage }));
    }
    setLoading(false);
  };

  return { loading, updateFormConfig };
};

export const useRemoveFormConfig = () => {
  const { t } = useTranslation();
  const datasetId = useDatasetId();
  const dispatch = useDispatch();
  const deleteFormConfig = useDeleteFormConfig();

  const [loading, setLoading] = useState(false);

  const removeFormConfig = async (configId: string) => {
    if (!datasetId) return;

    setLoading(true);
    try {
      await deleteFormConfig(datasetId, configId);

      dispatch(deleteFormConfigSuccess(configId));
    } catch (e) {
      dispatch(setMessage({ message: t("formConfig.deleteError") }));
    }
    setLoading(false);
  };

  return { loading, removeFormConfig };
};
