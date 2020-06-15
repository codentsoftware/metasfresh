import PropTypes from 'prop-types';
import React, { PureComponent, createRef } from 'react';
import classnames from 'classnames';
import MasterWidget from '../widget/MasterWidget';
import { getDateFormat, fieldValueToString } from '../../utils/tableHelpers';
import { DATE_FIELD_FORMATS } from '../../constants/Constants';
import WidgetTooltip from '../widget/WidgetTooltip';
class TableCell extends PureComponent {
  constructor(props) {
    super(props);
    this.widget = createRef();
    this.clearWidgetValue = false;

    this.state = {
      tooltipToggled: false, // keeping in the local state the flag for the tooltip
    };
  }

  /**
   * @method widgetTooltipToggle
   * @summary Alternative method to open dropdown, in case of disabled opening on focus.
   * @param {string|null} value
   */
  widgetTooltipToggle = (value) => {
    const curVal = this.state.tooltipToggled;
    const newVal = value != null ? value : !curVal;

    this.setState({ tooltipToggled: newVal });
  };

  /**
   * @method handleBackdropLock
   * @summary checks widget against widget list and calls parent onClickOutside fnct
   * @param {object} state
   */
  handleBackdropLock = (state) => {
    const { item } = this.props;
    const widgetsList = ['ProductAttributes', 'Attributes', 'List', 'Lookup'];
    if (!widgetsList.includes(item.widgetType)) {
      !state && this.props.onClickOutside();
    }
  };

  /**
   * @method handleKeyDown
   * @summary Key down function handler
   * @param {object} e
   */
  handleKeyDown = (e) => {
    const {
      handleKeyDown,
      property,
      item,
      getWidgetData,
      isEditable,
      supportFieldEdit,
      readonly,
      updateRow,
    } = this.props;
    const widgetData = getWidgetData(item, isEditable, supportFieldEdit);
    if (e.keyCode === 67 && (e.ctrlKey || e.metaKey)) {
      return false; // CMD + C on Mac has to just copy
    }
    handleKeyDown(e, property, widgetData[0]);
    !readonly && updateRow(); // toggle flag in parrent component highlighting the row giving the user the idea that something is happening with it
  };

  /**
   * @method handleRightClick
   * @summary Function called on right click that further calls the parent handler function to handleRightClick
   * @param {object} e
   */
  handleRightClick = (e) => {
    const {
      handleRightClick,
      property,
      supportZoomInto,
      supportFieldEdit,
      keyProperty,
    } = this.props;

    handleRightClick(
      e,
      keyProperty,
      property,
      !!supportZoomInto,
      supportFieldEdit
    );
  };

  /**
   * @method onDoubleClick
   * @summary Function called on double click that retrieves widget data and further calls the parent handler function to handleDounbleClick
   * @param {object} e
   */
  onDoubleClick = (e) => {
    const {
      property,
      item,
      getWidgetData,
      isEditable,
      supportFieldEdit,
      handleDoubleClick,
    } = this.props;
    const widgetData = getWidgetData(item, isEditable, supportFieldEdit);

    isEditable && handleDoubleClick(e, property, true, widgetData[0]);
  };

  /**
   * @method clearValue
   * @summary Set `clearWidgetValue` value based on a given `reset` param
   * {string|null} reset
   */
  clearValue = (reset) => {
    this.clearWidgetValue = reset === null ? true : false;
  };

  /**
   * @method render
   * @summary Main render function
   */
  render() {
    const {
      isEdited,
      isEditable,
      supportFieldEdit,
      cellExtended,
      extendLongText,
      getWidgetData,
      item,
      windowId,
      rowId,
      tabId,
      property,
      updatedRow,
      tabIndex,
      entity,
      listenOnKeys,
      listenOnKeysFalse,
      listenOnKeysTrue,
      closeTableField,
      getSizeClass,
      mainTable,
      onCellChange,
      viewId,
      modalVisible,
      onClickOutside,
      isGerman,
      activeLocale,
    } = this.props;
    const widgetData = getWidgetData(item, isEditable, supportFieldEdit);
    const docId = `${this.props.docId}`;
    const { tooltipToggled } = this.state;
    const tdValue = !isEdited
      ? fieldValueToString({
          fieldValue: widgetData[0].value,
          fieldType: item.widgetType,
          precision: widgetData[0].precision,
          isGerman,
          activeLocale,
        })
      : null;
    const description =
      widgetData[0].value && widgetData[0].value.description
        ? widgetData[0].value.description
        : tdValue;
    let tdTitle =
      item.widgetType === 'YesNo' ||
      item.widgetType === 'Switch' ||
      item.widgetType === 'Color'
        ? ''
        : description;
    const isOpenDatePicker = isEdited && item.widgetType === 'Date';
    const isDateField = DATE_FIELD_FORMATS[item.widgetType]
      ? getDateFormat(item.widgetType)
      : false;
    let style = {};
    let tooltipData = null;
    let tooltipWidget =
      item.fields && item.widgetType === 'Lookup'
        ? item.fields.find((field, idx) => {
            if (field.type === 'Tooltip') {
              tooltipData = widgetData[idx];

              if (tooltipData && tooltipData.value) {
                return field;
              }
            }
            return false;
          })
        : null;

    if (cellExtended) {
      style = {
        height: extendLongText * 20,
      };
    }

    let entityEffective;
    if (viewId) {
      entityEffective = 'documentView';
    } else if (mainTable) {
      entityEffective = 'window';
    } else {
      entityEffective = entity;
    }

    return (
      <td
        tabIndex={modalVisible ? -1 : tabIndex}
        ref={(c) => (this.cell = c)}
        onDoubleClick={this.onDoubleClick}
        onKeyDown={this.handleKeyDown}
        onContextMenu={this.handleRightClick}
        className={classnames(
          {
            [`text-${item.gridAlign}`]: item.gridAlign,
            'cell-disabled': widgetData[0].readonly,
            'cell-mandatory': widgetData[0].mandatory,
          },
          getSizeClass(item),
          item.widgetType,
          {
            'pulse-on': updatedRow,
            'pulse-off': !updatedRow,
          }
        )}
        data-cy={`cell-${property}`}
      >
        {isEdited ? (
          <MasterWidget
            {...item}
            {...{
              getWidgetData,
              viewId,
              rowId,
              widgetData,
              closeTableField,
              isOpenDatePicker,
              listenOnKeys,
              listenOnKeysFalse,
              listenOnKeysTrue,
              onClickOutside,
            }}
            clearValue={this.clearWidgetValue}
            entity={entityEffective}
            dateFormat={isDateField}
            dataId={mainTable ? null : docId}
            windowType={windowId}
            isMainTable={mainTable}
            tabId={mainTable ? null : tabId}
            noLabel={true}
            gridAlign={item.gridAlign}
            handleBackdropLock={this.handleBackdropLock}
            onChange={mainTable ? onCellChange : null}
            ref={this.widget}
          />
        ) : (
          <div className={classnames({ 'with-widget': tooltipWidget })}>
            <div
              className={classnames('cell-text-wrapper', {
                [`${item.widgetType.toLowerCase()}-cell`]: item.widgetType,
                extended: cellExtended,
              })}
              style={style}
              title={tdTitle}
            >
              {tdValue}
            </div>
            {tooltipWidget && !isEdited && (
              <WidgetTooltip
                widget={tooltipWidget}
                data={tooltipData}
                fieldName={item.field}
                isToggled={tooltipToggled}
                onToggle={this.widgetTooltipToggle}
              />
            )}
          </div>
        )}
      </td>
    );
  }
}

TableCell.propTypes = {
  tabId: PropTypes.any,
  windowId: PropTypes.any,
  keyProperty: PropTypes.string,
  tabIndex: PropTypes.number,
  listenOnKeys: PropTypes.bool,
  listenOnKeysFalse: PropTypes.func,
  listenOnKeysTrue: PropTypes.func,
  closeTableField: PropTypes.func,
  tdValue: PropTypes.any,
  supportFieldEdit: PropTypes.bool,
  supportZoomInto: PropTypes.bool,
  updatedRow: PropTypes.any,
  readonly: PropTypes.bool,
  rowId: PropTypes.string,
  item: PropTypes.object,
  isEditable: PropTypes.bool,
  updateRow: PropTypes.any,
  cellExtended: PropTypes.bool,
  extendLongText: PropTypes.number,
  property: PropTypes.string,
  getWidgetData: PropTypes.func,
  handleRightClick: PropTypes.func,
  handleKeyDown: PropTypes.func,
  handleDoubleClick: PropTypes.func,
  onClickOutside: PropTypes.func,
  onCellChange: PropTypes.func,
  isEdited: PropTypes.bool,
  isGerman: PropTypes.bool,
  entity: PropTypes.any,
  getSizeClass: PropTypes.func,
  mainTable: PropTypes.bool,
  viewId: PropTypes.string,
  modalVisible: PropTypes.bool,
  docId: PropTypes.any,
  activeLocale: PropTypes.object,
};

export default TableCell;
