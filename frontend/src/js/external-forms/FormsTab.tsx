import { tabDescription } from ".";
import { StateT } from "app-types";
import { useEffect, useMemo } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useSelector, useStore } from "react-redux";

import { useGetForms } from "../api/api";
import { DatasetIdT, DatasetT } from "../api/types";
import StandardQueryEditorTab from "../standard-query-editor";
import { updateReducers } from "../store";
import TimebasedQueryEditorTab from "../timebased-query-editor";

import FormContainer from "./FormContainer";
import FormsNavigation from "./FormsNavigation";
import FormsQueryRunner from "./FormsQueryRunner";
import { Form } from "./config-types";
import { DynamicFormValues } from "./form/Form";
import { collectAllFormFields, getInitialValue } from "./helper";
import buildExternalFormsReducer from "./reducer";
import { selectFormConfig } from "./stateSelectors";

const useLoadForms = () => {
  const store = useStore();
  const getForms = useGetForms();
  const datasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId,
  );

  useEffect(() => {
    async function loadForms() {
      if (!datasetId) {
        return;
      }

      const configuredForms = await getForms(datasetId);

      const forms = Object.fromEntries(
        configuredForms.map((form) => [form.type, form]),
      );

      const externalFormsReducer = buildExternalFormsReducer(forms);

      const tabs = [
        StandardQueryEditorTab,
        TimebasedQueryEditorTab,
        {
          ...tabDescription,
          reducer: externalFormsReducer,
        },
      ];

      updateReducers(store, tabs);
    }

    loadForms();
  }, [store, datasetId]);
};

const useInitializeForm = () => {
  const config = useSelector<StateT, Form | null>(selectFormConfig);
  const availableDatasets = useSelector<StateT, DatasetT[]>(
    (state) => state.datasets.data,
  );
  const datasetOptions = useMemo(
    () =>
      availableDatasets.map((dataset) => ({
        label: dataset.label,
        value: dataset.id,
      })),
    [availableDatasets],
  );
  const allFields = useMemo(() => {
    return config ? collectAllFormFields(config.fields) : [];
  }, [config]);

  const defaultValues = useMemo(
    () =>
      Object.fromEntries(
        allFields.map((field) => [
          field.name,
          getInitialValue(field, { availableDatasets: datasetOptions }),
        ]),
      ),
    [allFields, datasetOptions],
  );

  console.log(defaultValues);

  const methods = useForm<DynamicFormValues>({
    defaultValues,
    mode: "onChange",
  });

  return { methods, config, datasetOptions };
};

const FormsTab = () => {
  useLoadForms();

  const { methods, config, datasetOptions } = useInitializeForm();

  return (
    <FormProvider {...methods}>
      <FormsNavigation />
      <FormContainer
        methods={methods}
        config={config}
        datasetOptions={datasetOptions}
      />
      <FormsQueryRunner />
    </FormProvider>
  );
};

export default FormsTab;
