import styled from "@emotion/styled";
import { FC } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../../button/IconButton";
import WithTooltip from "../../tooltip/WithTooltip";

const FoldersButton = styled(IconButton)`
  padding: 8px 6px;
  margin-right: 5px;
`;

interface Props {
  className?: string;
  active?: boolean;
  onClick: () => void;
}

const FoldersToggleButton: FC<Props> = ({ className, active, onClick }) => {
  const { t } = useTranslation();

  return (
    <WithTooltip
      text={t("previousQueriesFolderButton.tooltip")}
      className={className}
    >
      <FoldersButton onClick={onClick} icon="folder" active={active} frame />
    </WithTooltip>
  );
};
export default FoldersToggleButton;
