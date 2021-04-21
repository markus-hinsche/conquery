import styled from "@emotion/styled";
import React, { ReactNode } from "react";
import { useTranslation } from "react-i18next";

import PrimaryButton from "../button/PrimaryButton";
import TransparentButton from "../button/TransparentButton";

import Modal from "./Modal";

const Root = styled("div")`
  text-align: center;
`;

const Btn = styled(TransparentButton)`
  margin: 0 10px;
`;

const PrimaryBtn = styled(PrimaryButton)`
  margin: 0 10px;
`;

interface PropsType {
  headline: ReactNode;
  onClose: () => void;
  onDelete: () => void;
}

const DeleteModal = ({ headline, onClose, onDelete }: PropsType) => {
  const { t } = useTranslation();

  return (
    <Modal onClose={onClose} headline={headline}>
      <Root>
        <Btn onClick={onClose}>{t("common.cancel")}</Btn>
        <PrimaryBtn onClick={onDelete}>{t("common.delete")}</PrimaryBtn>
      </Root>
    </Modal>
  );
};

export default DeleteModal;
