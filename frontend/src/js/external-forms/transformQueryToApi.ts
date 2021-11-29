import { transformElementsToApi } from "../api/apiHelper";
import type { SelectOptionT } from "../api/types";

import type { Form, FormField, GeneralField } from "./config-types";
import type { DynamicFormValues } from "./form/Form";

function transformElementGroupsToApi(elementGroups) {
  return elementGroups.map(({ concepts, connector, ...rest }) =>
    concepts.length > 1
      ? {
          type: connector,
          children: transformElementsToApi(concepts),
          ...rest,
        }
      : { ...transformElementsToApi(concepts)[0], ...rest },
  );
}

function transformFieldToApi(
  fieldConfig: FormField,
  formValues: DynamicFormValues,
) {
  const formValue = formValues[fieldConfig.name];

  switch (fieldConfig.type) {
    case "SELECT":
      // A RESULT_GROUP field may allow null / be optional
      return formValue ? (formValue as SelectOptionT).value : null;
    case "RESULT_GROUP":
      // A RESULT_GROUP field may allow null / be optional
      return formValue ? formValue.id : null;
    case "MULTI_RESULT_GROUP":
      return formValue.map((group) => group.id);
    case "DATE_RANGE":
      return {
        min: formValue.min,
        max: formValue.max,
      };
    case "CONCEPT_LIST":
      return transformElementGroupsToApi(formValue);
    case "TABS":
      const selectedTab = fieldConfig.tabs.find(
        (tab) => tab.name === formValue,
      );

      if (!selectedTab) {
        throw new Error(
          `No tab selected for ${fieldConfig.name}, this shouldn't happen`,
        );
      }

      return {
        value: formValue,
        // Only include field values from the selected tab
        ...transformFieldsToApi(selectedTab.fields, formValues),
      };
    default:
      return formValue;
  }
}

function transformFieldsToApi(
  fields: GeneralField[],
  formValues: DynamicFormValues,
) {
  return fields.reduce<DynamicFormValues>((all, fieldConfig) => {
    all[fieldConfig.name] = transformFieldToApi(fieldConfig, formValues);

    return all;
  }, {});
}

const transformQueryToApi =
  (formConfig: Form) => (formValues: DynamicFormValues) => {
    return {
      type: formConfig.type,
      ...transformFieldsToApi(formConfig.fields, formValues),
    };
  };

export default transformQueryToApi;
