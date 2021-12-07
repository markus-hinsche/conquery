import { StateT } from "app-types";
import { NodeResetConfig } from "js/model/node";
import { useEffect, useState } from "react";
import { useSelector } from "react-redux";

import type { PostPrefixForSuggestionsParams } from "../../api/api";
import {
  ConceptIdT,
  CurrencyConfigT,
  DatasetIdT,
  SelectOptionT,
  SelectorResultType,
} from "../../api/types";
import { toUpperCaseUnderscore } from "../../common/helpers";
import { usePrevious } from "../../common/helpers/usePrevious";
import { tableIsEditable } from "../../model/table";
import QueryNodeEditor from "../../query-node-editor/QueryNodeEditor";
import type {
  ConceptQueryNodeType,
  DragItemConceptTreeNode,
} from "../../standard-query-editor/types";
import type { ModeT } from "../../ui-components/InputRange";
import type { EditedFormQueryNodePosition } from "../form-concept-group/FormConceptGroup";
import { FormContextStateT } from "../reducer";
import { selectFormContextState } from "../stateSelectors";
import { initTables } from "../transformers";

interface PropsT {
  formType: string;
  fieldName: string;
  nodePosition: EditedFormQueryNodePosition;
  node: ConceptQueryNodeType;
  blocklistedTables?: string[];
  allowlistedTables?: string[];
  allowlistedSelects?: SelectorResultType[];
  blocklistedSelects?: SelectorResultType[];
  onCloseModal: () => void;
  onUpdateLabel: (label: string) => void;
  onToggleTable: (tableIdx: number, isExcluded: boolean) => void;
  onDropConcept: (concept: DragItemConceptTreeNode) => void;
  onRemoveConcept: (conceptId: ConceptIdT) => void;
  onSetFilterValue: (
    tableIdx: number,
    filterIdx: number,
    filterValue: any,
  ) => void;
  onSwitchFilterMode: (
    tableIdx: number,
    filterIdx: number,
    mode: ModeT,
  ) => void;
  onResetAllFilters: (config: NodeResetConfig) => void;
  onResetTable: (tableIdx: number, config: NodeResetConfig) => void;
  onSelectSelects: (selectedSelects: SelectOptionT[]) => void;
  onSelectTableSelects: (
    tableIdx: number,
    selectedSelects: SelectOptionT[],
  ) => void;
  onLoadFilterSuggestions: (
    params: PostPrefixForSuggestionsParams,
    tableIdx: number,
    filterIdx: number,
  ) => Promise<void>;
  onSetDateColumn: (tableIdx: number, dateColumnValue: string | null) => void;
}

const FormQueryNodeEditor = (props: PropsT) => {
  const datasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId,
  );

  const [editedNode, setEditedNode] = useState(props.node);

  const previousNodePosition = usePrevious(props.nodePosition);
  useEffect(
    function () {
      if (previousNodePosition !== props.nodePosition) {
        setEditedNode(
          initTables({
            blocklistedTables: props.blocklistedTables,
            allowlistedTables: props.allowlistedTables,
          })(props.node),
        );
      }
    },
    [
      previousNodePosition,
      props.nodePosition,
      props.node,
      props.blocklistedTables,
      props.allowlistedTables,
    ],
  );

  useEffect(
    function syncWithNodeFromOutside() {
      setEditedNode(props.node);
    },
    [props.node],
  );

  const showTables =
    !!editedNode &&
    editedNode.tables &&
    editedNode.tables.length > 1 &&
    editedNode.tables.some((table) => tableIsEditable(table));

  const formState = useSelector<StateT, FormContextStateT | null>((state) =>
    selectFormContextState(state, props.formType),
  );

  const currencyConfig = useSelector<StateT, CurrencyConfigT>(
    (state) => state.startup.config.currency,
  );
  const editorState = formState ? formState[props.fieldName] : null;

  if (!datasetId || !editorState) {
    return null;
  }

  return (
    <QueryNodeEditor
      datasetId={datasetId}
      name={`${props.formType}_${toUpperCaseUnderscore(props.fieldName)}`}
      onLoadFilterSuggestions={props.onLoadFilterSuggestions}
      node={editedNode}
      editorState={editorState}
      showTables={showTables}
      blocklistedTables={props.blocklistedTables}
      allowlistedTables={props.allowlistedTables}
      blocklistedSelects={props.blocklistedSelects}
      allowlistedSelects={props.allowlistedSelects}
      currencyConfig={currencyConfig}
      onCloseModal={props.onCloseModal}
      onUpdateLabel={props.onUpdateLabel}
      onDropConcept={props.onDropConcept}
      onRemoveConcept={props.onRemoveConcept}
      onToggleTable={props.onToggleTable}
      onSelectSelects={props.onSelectSelects}
      onSelectTableSelects={props.onSelectTableSelects}
      onSetFilterValue={props.onSetFilterValue}
      onSwitchFilterMode={props.onSwitchFilterMode}
      onResetTable={props.onResetTable}
      onResetAllFilters={props.onResetAllFilters}
      onSetDateColumn={props.onSetDateColumn}
    />
  );
};

export default FormQueryNodeEditor;
