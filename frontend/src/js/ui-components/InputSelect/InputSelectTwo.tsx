import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { useCombobox } from "downshift";
import { useState, useEffect, useRef, useCallback } from "react";
import { useTranslation } from "react-i18next";

import type { SelectOptionT } from "../../api/types";
import IconButton from "../../button/IconButton";
import { exists } from "../../common/helpers/exists";
import { useClickOutside } from "../../common/helpers/useClickOutside";
import { usePrevious } from "../../common/helpers/usePrevious";
import InfoTooltip from "../../tooltip/InfoTooltip";
import Labeled from "../Labeled";
import SelectEmptyPlaceholder from "../SelectEmptyPlaceholder";
import SelectListOption from "../SelectListOption";

const Control = styled("div")<{ disabled?: boolean }>`
  border: 1px solid ${({ theme }) => theme.col.gray};
  border-radius: 4px;
  display: flex;
  align-items: center;
  overflow: hidden;
  padding: 3px 3px 3px 8px;
  background-color: white;
  ${({ disabled }) =>
    disabled &&
    css`
      cursor: not-allowed;
    `}

  &:focus {
    outline: 1px solid black;
  }
`;

const SelectContainer = styled("div")`
  width: 100%;
  position: relative;
`;

const ItemsInputContainer = styled("div")`
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 3px;
  width: 100%;
`;

const Menu = styled("div")`
  position: absolute;
  width: 100%;
  border-radius: 4px;
  box-shadow: 0 0 0 1px hsl(0deg 0% 0% / 10%), 0 4px 11px hsl(0deg 0% 0% / 10%);
  background-color: ${({ theme }) => theme.col.bg};
  z-index: 2;
`;

const List = styled("div")`
  padding: 3px;
  max-height: 300px;
  overflow-y: auto;
  --webkit-overflow-scrolling: touch;
`;

const Input = styled("input")`
  border: 0;
  height: 20px;
  outline: none;
  flex-grow: 1;
  flex-basis: 30px;
  ${({ disabled }) =>
    disabled &&
    css`
      cursor: not-allowed;
      pointer-events: none;
      &:placehoder {
        opacity: 0.5;
      }
    `}
`;

const SxLabeled = styled(Labeled)`
  padding: 2px;
`;

const DropdownToggleButton = styled(IconButton)`
  padding: 3px 6px;
`;

const ResetButton = styled(IconButton)`
  padding: 3px 8px;
`;

const VerticalSeparator = styled("div")`
  width: 1px;
  margin: 3px 0;
  background-color: ${({ theme }) => theme.col.grayVeryLight};
  align-self: stretch;
  flex-shrink: 0;
`;

interface Props {
  label?: string;
  disabled?: boolean;
  options: SelectOptionT[];
  tooltip?: string;
  indexPrefix?: number;
  placeholder?: string;
  loading?: boolean;
  input: {
    value: SelectOptionT | null;
    defaultValue?: SelectOptionT["value"] | null;
    onChange: (value: SelectOptionT | null) => void;
  };
}

const InputSelectTwo = ({
  options,
  placeholder,
  input,
  label,
  tooltip,
  indexPrefix,
  disabled,
}: Props) => {
  const { t } = useTranslation();
  const previousInputValue = usePrevious(input.value);
  const inputRef = useRef<HTMLInputElement | null>(null);

  const [filteredOptions, setFilteredOptions] = useState(() => {
    if (!input.value) return options;

    return options.some((option) => option.value === input.value?.value)
      ? options
      : [input.value, ...options];
  });

  const defaultOption =
    input.value ||
    filteredOptions.find((option) => option.value === input.defaultValue);

  const {
    isOpen,
    toggleMenu,
    getToggleButtonProps,
    getLabelProps,
    getMenuProps,
    getInputProps,
    getComboboxProps,
    getItemProps,
    highlightedIndex,
    setHighlightedIndex,
    selectedItem,
    selectItem,
    reset: resetComboboxState,
    inputValue,
    setInputValue,
  } = useCombobox({
    itemToString: (item) => {
      return item?.label || "";
    },
    defaultSelectedItem: defaultOption,
    items: filteredOptions,
    stateReducer: (state, { type, changes }) => {
      // This modifies the action payload itself
      // in that way
      // - the default behavior may be adjusted
      // - including the `onStateChange` reactions that diverge from default behavior (see below)
      switch (type) {
        case useCombobox.stateChangeTypes.InputKeyDownEnter:
        case useCombobox.stateChangeTypes.ItemClick:
          if (inputRef.current) {
            inputRef.current.blur();
          }
          return state;
        case useCombobox.stateChangeTypes.InputBlur:
          if (changes.selectedItem?.disabled) {
            return state;
          }

          return {
            ...changes,
            inputValue: String(changes.selectedItem?.label || ""),
          };
        default:
          return changes;
      }
    },
    onInputValueChange: (changes) => {
      if (changes.highlightedIndex !== 0) {
        setHighlightedIndex(0);
      }

      if (!exists(changes.inputValue)) {
        return;
      }

      setFilteredOptions(
        options.filter((option) => {
          if (!changes.inputValue) return true;

          const lowerInputValue = changes.inputValue.toLowerCase();
          const lowerLabel = option.label.toLowerCase();

          return (
            lowerLabel.includes(lowerInputValue) ||
            String(option.value).toLowerCase().includes(lowerInputValue)
          );
        }),
      );
    },
    onStateChange: ({ type, ...changes }) => {
      // This only modifies the behavior of some of the actions, after the state has been changed
      switch (type) {
        case useCombobox.stateChangeTypes.InputKeyDownEscape:
          if (changes.isOpen) {
            // Sometimes closing the menu on esc didn't work, this fixes it
            toggleMenu();
          }
          break;
        case useCombobox.stateChangeTypes.InputBlur:
          setFilteredOptions(options);

          if (changes.selectedItem) {
            input.onChange(changes.selectedItem);
          }
          break;
        default:
          break;
      }
    },
  });

  const { ref: menuPropsRef, ...menuProps } = getMenuProps();
  const { ref: inputPropsRef, ...inputProps } = getInputProps();
  const { ref: comboboxRef, ...comboboxProps } = getComboboxProps();
  const labelProps = getLabelProps();

  const handleBlur = useCallback(() => {
    if (!!selectedItem && inputValue !== selectedItem.label) {
      setInputValue(selectedItem.label);
    }
  }, [inputValue, setInputValue, selectedItem]);

  const clickOutsideRef = useRef<HTMLLabelElement>(null);
  useClickOutside(
    clickOutsideRef,
    useCallback(() => {
      if (isOpen) {
        toggleMenu();
        handleBlur();
      }
    }, [isOpen, toggleMenu, handleBlur]),
  );

  useEffect(() => {
    if (
      exists(input.value) &&
      previousInputValue !== input.value &&
      input.value.value !== selectedItem?.value
    ) {
      selectItem(input.value);
    }
  }, [previousInputValue, selectedItem, selectItem, input.value]);

  return (
    <SxLabeled
      {...labelProps}
      ref={clickOutsideRef}
      htmlFor="" // Important to override getLabelProps with this to avoid click events everywhere
      label={
        <>
          {label}
          {tooltip && <InfoTooltip text={tooltip} />}
        </>
      }
      indexPrefix={indexPrefix}
    >
      <SelectContainer>
        <Control
          {...comboboxProps}
          disabled={disabled}
          ref={(instance) => {
            comboboxRef(instance);
          }}
        >
          <ItemsInputContainer>
            <Input
              {...inputProps}
              ref={(instance) => {
                inputPropsRef(instance);
                inputRef.current = instance;
              }}
              type="text"
              disabled={disabled}
              placeholder={placeholder || t("inputSelect.placeholder")}
              onClick={(e) => {
                if (inputProps.onClick) {
                  inputProps.onClick(e);
                }
                toggleMenu();
              }}
              onChange={(e) => {
                if (inputProps.onChange) {
                  inputProps.onChange(e);
                }
                setInputValue(e.target.value);
              }}
            />
          </ItemsInputContainer>
          {(inputValue.length > 0 || exists(selectedItem)) && (
            <ResetButton
              icon="times"
              disabled={disabled}
              onClick={() => {
                resetComboboxState();
                input.onChange(null);
              }}
            />
          )}
          <VerticalSeparator />
          <DropdownToggleButton
            disabled={disabled}
            icon="chevron-down"
            {...getToggleButtonProps()}
          />
        </Control>
        {isOpen ? (
          <Menu
            {...menuProps}
            ref={(instance) => {
              menuPropsRef(instance);
            }}
          >
            <List>
              {filteredOptions.length === 0 && <SelectEmptyPlaceholder />}
              {filteredOptions.map((option, index) => {
                const { ref: itemPropsRef, ...itemProps } = getItemProps({
                  index,
                  item: filteredOptions[index],
                });

                return (
                  <SelectListOption
                    key={`${option.value}`}
                    active={highlightedIndex === index}
                    disabled={option.disabled}
                    {...itemProps}
                    ref={(instance) => {
                      itemPropsRef(instance);
                    }}
                  >
                    {option.label}
                  </SelectListOption>
                );
              })}
            </List>
          </Menu>
        ) : (
          <span ref={menuPropsRef} /> // To avoid a warning / error by downshift that ref is not applied
        )}
      </SelectContainer>
    </SxLabeled>
  );
};

export default InputSelectTwo;
