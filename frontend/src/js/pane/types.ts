import { ComponentType } from "react";

export type SupportedTabsT = "timebasedQueryEditor" | "queryEditor";

// Used for right pane tabs at the moment
export type TabT = {
  key: string;
  labelKey: string; // Translatable key, yes, not ideal that it's dynamic
  tooltipKey: string; // Translatable key, yes, not ideal that it's dynamic
  component: ComponentType<any>; // The tab contents
};
