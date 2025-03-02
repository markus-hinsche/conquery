import styled from "@emotion/styled";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";

import { useGetQuery } from "../../api/api";
import { useDatasetId } from "../../dataset/selectors";
import type { DragItemQuery } from "../../standard-query-editor/types";

import FormQueryResult from "./FormQueryResult";

const FullWidthCentered = styled("div")`
  width: 100%;
  text-align: center;
`;
interface PropsT {
  queryResult?: DragItemQuery;
  placeholder: string;
  className?: string;
  onDelete?: () => void;
  onInvalid: (error: string) => void;
}

const ValidatedFormQueryResult = ({
  onInvalid,
  placeholder,
  queryResult,
  ...props
}: PropsT) => {
  const datasetId = useDatasetId();
  const getQuery = useGetQuery();
  const { t } = useTranslation();

  const [localError, setLocalError] = useState<boolean>(false);

  useEffect(
    function validateQuery() {
      const loadAndValidateQuery = async () => {
        if (datasetId && queryResult) {
          try {
            await getQuery(datasetId, queryResult.id);
            setLocalError(false);
          } catch (e) {
            setLocalError(true);
            onInvalid(t("previousQuery.loadError"));
          }
        }
      };

      loadAndValidateQuery();
    },
    [datasetId, queryResult],
  );

  const error = localError ? t("previousQuery.loadError") : undefined;

  return !queryResult && !error ? (
    <FullWidthCentered>{placeholder}</FullWidthCentered>
  ) : (
    <FormQueryResult
      {...props}
      queryResult={queryResult}
      onDelete={() => {
        props.onDelete?.();
        setLocalError(false);
      }}
      error={error}
    />
  );
};

export default ValidatedFormQueryResult;
