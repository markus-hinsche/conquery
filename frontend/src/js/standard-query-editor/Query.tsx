import React, { FC, useState } from "react";
import styled from "@emotion/styled";

import { useDispatch, useSelector } from "react-redux";
import { useTranslation } from "react-i18next";

import type { DatasetIdT } from "../api/types";
import { exists } from "../common/helpers/exists";

import { queryGroupModalSetNode } from "../query-group-modal/actions";
import { useLoadPreviousQuery } from "../previous-queries/list/actions";
import { openQueryUploadConceptListModal } from "../query-upload-concept-list-modal/actions";

import {
  dropAndNode,
  dropOrNode,
  deleteNode,
  deleteGroup,
  toggleExcludeGroup,
  useExpandPreviousQuery,
  selectNodeForEditing,
  toggleTimestamps,
  toggleSecondaryIdExclude,
} from "./actions";

import type {
  StandardQueryType,
  DraggedNodeType,
  DraggedQueryType,
  PreviousQueryQueryNodeType,
} from "./types";
import QueryEditorDropzone from "./QueryEditorDropzone";
import QueryGroup from "./QueryGroup";
import { StateT } from "app-types";
import { TreesT } from "../concept-trees/reducer";
import { PreviousQueryIdT } from "../previous-queries/list/reducer";
import QueryHeader from "./QueryHeader";
import QueryFooter from "./QueryFooter";
import ExpandPreviousQueryModal from "./ExpandPreviousQueryModal";

const Container = styled("div")`
  height: 100%;
  display: flex;
  flex-direction: column;
`;

const Groups = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  padding: 0 0 20px;
  flex-grow: 1;
`;

const QueryGroupConnector = styled("p")`
  padding: 110px 6px 0;
  margin: 0;
  font-size: ${({ theme }) => theme.font.sm};
  color: ${({ theme }) => theme.col.gray};
  text-align: center;
`;

interface PropsT {
  selectedDatasetId: DatasetIdT;
}

const Query: FC<PropsT> = ({ selectedDatasetId }) => {
  const { t } = useTranslation();
  const query = useSelector<StateT, StandardQueryType>(
    (state) => state.queryEditor.query
  );
  const isEmptyQuery = useSelector<StateT, boolean>(
    (state) => state.queryEditor.query.length === 0
  );

  // only used by other actions
  const rootConcepts = useSelector<StateT, TreesT>(
    (state) => state.conceptTrees.trees
  );

  const dispatch = useDispatch();
  const loadPreviousQuery = useLoadPreviousQuery();
  const expandPreviousQuery = useExpandPreviousQuery();

  const onDropAndNode = (item: DraggedNodeType | DraggedQueryType) =>
    dispatch(dropAndNode(item));
  const onDropConceptListFile = (file: File, andIdx: number | null) =>
    dispatch(openQueryUploadConceptListModal(andIdx, file));
  const onDropOrNode = (
    item: DraggedNodeType | DraggedQueryType,
    andIdx: number
  ) => dispatch(dropOrNode(item, andIdx));
  const onDeleteNode = (andIdx: number, orIdx: number) =>
    dispatch(deleteNode(andIdx, orIdx));
  const onDeleteGroup = (andIdx: number) => dispatch(deleteGroup(andIdx));
  const onToggleExcludeGroup = (andIdx: number) =>
    dispatch(toggleExcludeGroup(andIdx));
  const onToggleTimestamps = (andIdx: number, orIdx: number) =>
    dispatch(toggleTimestamps(andIdx, orIdx));
  const onToggleSecondaryIdExclude = (andIdx: number, orIdx: number) =>
    dispatch(toggleSecondaryIdExclude(andIdx, orIdx));
  const onSelectNodeForEditing = (andIdx: number, orIdx: number) =>
    dispatch(selectNodeForEditing(andIdx, orIdx));
  const onQueryGroupModalSetNode = (andIdx: number) =>
    dispatch(queryGroupModalSetNode(andIdx));
  const onLoadPreviousQuery = (queryId: PreviousQueryIdT) =>
    loadPreviousQuery(selectedDatasetId, queryId);

  const [
    queryToExpand,
    setQueryToExpand,
  ] = useState<PreviousQueryQueryNodeType | null>(null);

  const onExpandPreviousQuery = (q: PreviousQueryQueryNodeType) =>
    setQueryToExpand(q);

  return (
    <Container>
      {exists(queryToExpand) && (
        <ExpandPreviousQueryModal
          onClose={() => setQueryToExpand(null)}
          onAccept={() => {
            expandPreviousQuery(selectedDatasetId, rootConcepts, queryToExpand);
            setQueryToExpand(null);
          }}
        />
      )}
      {isEmptyQuery ? (
        <QueryEditorDropzone
          isInitial
          onDropNode={onDropAndNode}
          onDropFile={(file) => onDropConceptListFile(file, null)}
          onLoadPreviousQuery={onLoadPreviousQuery}
        />
      ) : (
        <>
          <QueryHeader />
          <Groups>
            {query.map((group, andIdx) => [
              <QueryGroup
                key={andIdx}
                group={group}
                andIdx={andIdx}
                onDropNode={(item) => onDropOrNode(item, andIdx)}
                onDropFile={(file: File) => onDropConceptListFile(file, andIdx)}
                onDeleteNode={(orIdx: number) => onDeleteNode(andIdx, orIdx)}
                onDeleteGroup={() => onDeleteGroup(andIdx)}
                onEditClick={(orIdx: number) =>
                  onSelectNodeForEditing(andIdx, orIdx)
                }
                onExpandClick={onExpandPreviousQuery}
                onExcludeClick={() => onToggleExcludeGroup(andIdx)}
                onDateClick={() => onQueryGroupModalSetNode(andIdx)}
                onLoadPreviousQuery={onLoadPreviousQuery}
                onToggleTimestamps={(orIdx: number) =>
                  onToggleTimestamps(andIdx, orIdx)
                }
                onToggleSecondaryIdExclude={(orIdx: number) =>
                  onToggleSecondaryIdExclude(andIdx, orIdx)
                }
              />,
              <QueryGroupConnector key={`${andIdx}.and`}>
                {t("common.and")}
              </QueryGroupConnector>,
            ])}
            <QueryEditorDropzone
              isAnd
              onDropNode={onDropAndNode}
              onDropFile={(file) => onDropConceptListFile(file, null)}
              onLoadPreviousQuery={onLoadPreviousQuery}
            />
          </Groups>
          <QueryFooter />
        </>
      )}
    </Container>
  );
};

export default Query;
