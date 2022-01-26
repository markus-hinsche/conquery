import { transformElementsToApi } from "../api/apiHelper";
import type { SelectOptionT } from "../api/types";
import type { DateStringMinMax } from "../common/helpers";
import type { DragItemQuery } from "../standard-query-editor/types";

import type { Form, GeneralField } from "./config-types";
import type { DynamicFormValues } from "./form/Form";
import { isFormField } from "./helper";

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

function transformFieldToApiEntries(
  fieldConfig: GeneralField,
  formValues: DynamicFormValues,
): [string, any][] {
  if (!isFormField(fieldConfig)) {
    return [];
  }

  const formValue =
    fieldConfig.type === "GROUP" ? null : formValues[fieldConfig.name];

  switch (fieldConfig.type) {
    case "CHECKBOX":
      return [[fieldConfig.name, formValue || false]];
    case "STRING":
    case "NUMBER":
      return [[fieldConfig.name, formValue || null]];
    case "DATASET_SELECT":
    case "SELECT":
      return [
        [
          fieldConfig.name,
          formValue ? (formValue as SelectOptionT).value : null,
        ],
      ];
    case "RESULT_GROUP":
      // A RESULT_GROUP field may allow null / be optional
      return [
        [fieldConfig.name, formValue ? (formValue as DragItemQuery).id : null],
      ];
    case "MULTI_RESULT_GROUP":
      return [
        [
          fieldConfig.name,
          (formValue as DragItemQuery[]).map((group) => group.id),
        ],
      ];
    case "DATE_RANGE":
      return [
        [
          fieldConfig.name,
          {
            min: (formValue as DateStringMinMax).min,
            max: (formValue as DateStringMinMax).max,
          },
        ],
      ];
    case "CONCEPT_LIST":
      return [[fieldConfig.name, transformElementGroupsToApi(formValue)]];
    case "GROUP":
      return fieldConfig.fields.flatMap((f) =>
        transformFieldToApiEntries(f, formValues),
      );
    case "TABS":
      const selectedTab = fieldConfig.tabs.find(
        (tab) => tab.name === formValue,
      );

      if (!selectedTab) {
        throw new Error(
          `No tab selected for ${fieldConfig.name}, this shouldn't happen`,
        );
      }

      return [
        [
          fieldConfig.name,
          {
            value: formValue,
            // Only include field values from the selected tab
            ...transformFieldsToApi(selectedTab.fields, formValues),
          },
        ],
      ];
  }
}

function transformFieldsToApi(
  fields: GeneralField[],
  formValues: DynamicFormValues,
): DynamicFormValues {
  return Object.fromEntries(
    fields.flatMap((field) => transformFieldToApiEntries(field, formValues)),
  );
}

const transformQueryToApi =
  (formConfig: Form) => (formValues: DynamicFormValues) => {
    return {
      type: formConfig.type,
      ...transformFieldsToApi(formConfig.fields, formValues),
    };
  };

export default transformQueryToApi;
