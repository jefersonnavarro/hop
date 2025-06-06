/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.pipeline.transforms.salesforcedelete;

import org.apache.hop.core.Const;
import org.apache.hop.core.Props;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transforms.salesforce.SalesforceConnection;
import org.apache.hop.pipeline.transforms.salesforce.SalesforceConnectionUtils;
import org.apache.hop.pipeline.transforms.salesforce.SalesforceTransformDialog;
import org.apache.hop.pipeline.transforms.salesforce.SalesforceTransformMeta;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.dialog.BaseDialog;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.core.gui.GuiResource;
import org.apache.hop.ui.core.widget.ComboVar;
import org.apache.hop.ui.core.widget.LabelTextVar;
import org.apache.hop.ui.core.widget.TextVar;
import org.apache.hop.ui.pipeline.transform.ComponentSelectionListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class SalesforceDeleteDialog extends SalesforceTransformDialog {

  private static final Class<?> PKG = SalesforceDeleteMeta.class;

  private SalesforceDeleteMeta input;

  private LabelTextVar wUserName;
  private LabelTextVar wURL;
  private LabelTextVar wPassword;

  private TextVar wBatchSize;

  private ComboVar wModule;

  private ComboVar wDeleteField;

  private boolean gotPrevious = false;

  private boolean gotModule = false;

  private boolean getModulesListError = false; /* True if error getting modules list */

  private Button wUseCompression;

  private TextVar wTimeOut;

  private Button wRollbackAllChangesOnError;

  public SalesforceDeleteDialog(
      Shell parent,
      IVariables variables,
      SalesforceDeleteMeta transformMeta,
      PipelineMeta pipelineMeta) {
    super(parent, variables, transformMeta, pipelineMeta);
    input = transformMeta;
  }

  @Override
  public String open() {
    Shell parent = getParent();

    shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
    PropsUi.setLook(shell);
    setShellImage(shell, input);

    ModifyListener lsMod = e -> input.setChanged();
    ModifyListener lsTableMod = arg0 -> input.setChanged();

    changed = input.hasChanged();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = PropsUi.getFormMargin();
    formLayout.marginHeight = PropsUi.getFormMargin();

    shell.setLayout(formLayout);
    shell.setText(BaseMessages.getString(PKG, "SalesforceDeleteDialog.DialogTitle"));

    int middle = props.getMiddlePct();
    int margin = PropsUi.getMargin();

    // TransformName line
    wlTransformName = new Label(shell, SWT.RIGHT);
    wlTransformName.setText(BaseMessages.getString(PKG, "System.TransformName.Label"));
    wlTransformName.setToolTipText(BaseMessages.getString(PKG, "System.TransformName.Tooltip"));
    PropsUi.setLook(wlTransformName);
    fdlTransformName = new FormData();
    fdlTransformName.left = new FormAttachment(0, 0);
    fdlTransformName.top = new FormAttachment(0, margin);
    fdlTransformName.right = new FormAttachment(middle, -margin);
    wlTransformName.setLayoutData(fdlTransformName);
    wTransformName = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wTransformName.setText(transformName);
    PropsUi.setLook(wTransformName);
    wTransformName.addModifyListener(lsMod);
    fdTransformName = new FormData();
    fdTransformName.left = new FormAttachment(middle, 0);
    fdTransformName.top = new FormAttachment(0, margin);
    fdTransformName.right = new FormAttachment(100, 0);
    wTransformName.setLayoutData(fdTransformName);

    CTabFolder wTabFolder = new CTabFolder(shell, SWT.BORDER);
    PropsUi.setLook(wTabFolder, Props.WIDGET_STYLE_TAB);

    // ////////////////////////
    // START OF FILE TAB ///
    // ////////////////////////
    CTabItem wGeneralTab = new CTabItem(wTabFolder, SWT.NONE);
    wGeneralTab.setFont(GuiResource.getInstance().getFontDefault());
    wGeneralTab.setText(BaseMessages.getString(PKG, "SalesforceDeleteDialog.General.Tab"));

    Composite wGeneralComp = new Composite(wTabFolder, SWT.NONE);
    PropsUi.setLook(wGeneralComp);

    FormLayout generalLayout = new FormLayout();
    generalLayout.marginWidth = 3;
    generalLayout.marginHeight = 3;
    wGeneralComp.setLayout(generalLayout);

    // ///////////////////////////////
    // START OF Connection GROUP //
    // ///////////////////////////////

    Group wConnectionGroup = new Group(wGeneralComp, SWT.SHADOW_NONE);
    PropsUi.setLook(wConnectionGroup);
    wConnectionGroup.setText(
        BaseMessages.getString(PKG, "SalesforceDeleteDialog.ConnectionGroup.Label"));

    FormLayout connectionGroupLayout = new FormLayout();
    connectionGroupLayout.marginWidth = 10;
    connectionGroupLayout.marginHeight = 10;
    wConnectionGroup.setLayout(connectionGroupLayout);

    // Webservice URL
    wURL =
        new LabelTextVar(
            variables,
            wConnectionGroup,
            BaseMessages.getString(PKG, "SalesforceDeleteDialog.URL.Label"),
            BaseMessages.getString(PKG, "SalesforceDeleteDialog.URL.Tooltip"));
    PropsUi.setLook(wURL);
    wURL.addModifyListener(lsMod);
    FormData fdURL = new FormData();
    fdURL.left = new FormAttachment(0, 0);
    fdURL.top = new FormAttachment(wTransformName, margin);
    fdURL.right = new FormAttachment(100, 0);
    wURL.setLayoutData(fdURL);

    // UserName line
    wUserName =
        new LabelTextVar(
            variables,
            wConnectionGroup,
            BaseMessages.getString(PKG, "SalesforceDeleteDialog.User.Label"),
            BaseMessages.getString(PKG, "SalesforceDeleteDialog.User.Tooltip"));
    PropsUi.setLook(wUserName);
    wUserName.addModifyListener(lsMod);
    FormData fdUserName = new FormData();
    fdUserName.left = new FormAttachment(0, 0);
    fdUserName.top = new FormAttachment(wURL, margin);
    fdUserName.right = new FormAttachment(100, 0);
    wUserName.setLayoutData(fdUserName);

    // Password line
    wPassword =
        new LabelTextVar(
            variables,
            wConnectionGroup,
            BaseMessages.getString(PKG, "SalesforceDeleteDialog.Password.Label"),
            BaseMessages.getString(PKG, "SalesforceDeleteDialog.Password.Tooltip"),
            true);
    PropsUi.setLook(wPassword);
    wPassword.addModifyListener(lsMod);
    FormData fdPassword = new FormData();
    fdPassword.left = new FormAttachment(0, 0);
    fdPassword.top = new FormAttachment(wUserName, margin);
    fdPassword.right = new FormAttachment(100, 0);
    wPassword.setLayoutData(fdPassword);

    // Test Salesforce connection button
    Button wTest = new Button(wConnectionGroup, SWT.PUSH);
    wTest.setText(BaseMessages.getString(PKG, "SalesforceDeleteDialog.TestConnection.Label"));
    PropsUi.setLook(wTest);
    FormData fdTest = new FormData();
    wTest.setToolTipText(
        BaseMessages.getString(PKG, "SalesforceDeleteDialog.TestConnection.Tooltip"));
    fdTest.top = new FormAttachment(wPassword, margin);
    fdTest.right = new FormAttachment(100, 0);
    wTest.setLayoutData(fdTest);

    FormData fdConnectionGroup = new FormData();
    fdConnectionGroup.left = new FormAttachment(0, margin);
    fdConnectionGroup.top = new FormAttachment(wTransformName, margin);
    fdConnectionGroup.right = new FormAttachment(100, -margin);
    wConnectionGroup.setLayoutData(fdConnectionGroup);

    // ///////////////////////////////
    // END OF Connection GROUP //
    // ///////////////////////////////

    // ///////////////////////////////
    // START OF Settings GROUP //
    // ///////////////////////////////

    Group wSettingsGroup = new Group(wGeneralComp, SWT.SHADOW_NONE);
    PropsUi.setLook(wSettingsGroup);
    wSettingsGroup.setText(
        BaseMessages.getString(PKG, "SalesforceDeleteDialog.SettingsGroup.Label"));

    FormLayout settingGroupLayout = new FormLayout();
    settingGroupLayout.marginWidth = 10;
    settingGroupLayout.marginHeight = 10;
    wSettingsGroup.setLayout(settingGroupLayout);

    // Timeout
    Label wlTimeOut = new Label(wSettingsGroup, SWT.RIGHT);
    wlTimeOut.setText(BaseMessages.getString(PKG, "SalesforceDeleteDialog.TimeOut.Label"));
    PropsUi.setLook(wlTimeOut);
    FormData fdlTimeOut = new FormData();
    fdlTimeOut.left = new FormAttachment(0, 0);
    fdlTimeOut.top = new FormAttachment(wSettingsGroup, margin);
    fdlTimeOut.right = new FormAttachment(middle, -margin);
    wlTimeOut.setLayoutData(fdlTimeOut);
    wTimeOut = new TextVar(variables, wSettingsGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wTimeOut);
    wTimeOut.addModifyListener(lsMod);
    FormData fdTimeOut = new FormData();
    fdTimeOut.left = new FormAttachment(middle, 0);
    fdTimeOut.top = new FormAttachment(wSettingsGroup, margin);
    fdTimeOut.right = new FormAttachment(100, 0);
    wTimeOut.setLayoutData(fdTimeOut);

    // Use compression?
    Label wlUseCompression = new Label(wSettingsGroup, SWT.RIGHT);
    wlUseCompression.setText(
        BaseMessages.getString(PKG, "SalesforceDeleteDialog.UseCompression.Label"));
    PropsUi.setLook(wlUseCompression);
    FormData fdlUseCompression = new FormData();
    fdlUseCompression.left = new FormAttachment(0, 0);
    fdlUseCompression.top = new FormAttachment(wTimeOut, margin);
    fdlUseCompression.right = new FormAttachment(middle, -margin);
    wlUseCompression.setLayoutData(fdlUseCompression);
    wUseCompression = new Button(wSettingsGroup, SWT.CHECK);
    PropsUi.setLook(wUseCompression);
    wUseCompression.setToolTipText(
        BaseMessages.getString(PKG, "SalesforceDeleteDialog.UseCompression.Tooltip"));
    FormData fdUseCompression = new FormData();
    fdUseCompression.left = new FormAttachment(middle, 0);
    fdUseCompression.top = new FormAttachment(wlUseCompression, 0, SWT.CENTER);
    wUseCompression.setLayoutData(fdUseCompression);
    wUseCompression.addSelectionListener(new ComponentSelectionListener(input));

    // Rollback all changes on error?
    Label wlRollbackAllChangesOnError = new Label(wSettingsGroup, SWT.RIGHT);
    wlRollbackAllChangesOnError.setText(
        BaseMessages.getString(PKG, "SalesforceDeleteDialog.RollbackAllChangesOnError.Label"));
    PropsUi.setLook(wlRollbackAllChangesOnError);
    FormData fdlRollbackAllChangesOnError = new FormData();
    fdlRollbackAllChangesOnError.left = new FormAttachment(0, 0);
    fdlRollbackAllChangesOnError.top = new FormAttachment(wUseCompression, margin);
    fdlRollbackAllChangesOnError.right = new FormAttachment(middle, -margin);
    wlRollbackAllChangesOnError.setLayoutData(fdlRollbackAllChangesOnError);
    wRollbackAllChangesOnError = new Button(wSettingsGroup, SWT.CHECK);
    PropsUi.setLook(wRollbackAllChangesOnError);
    wRollbackAllChangesOnError.setToolTipText(
        BaseMessages.getString(PKG, "SalesforceDeleteDialog.RollbackAllChangesOnError.Tooltip"));
    FormData fdRollbackAllChangesOnError = new FormData();
    fdRollbackAllChangesOnError.left = new FormAttachment(middle, 0);
    fdRollbackAllChangesOnError.top =
        new FormAttachment(wlRollbackAllChangesOnError, 0, SWT.CENTER);
    wRollbackAllChangesOnError.setLayoutData(fdRollbackAllChangesOnError);
    wRollbackAllChangesOnError.addSelectionListener(new ComponentSelectionListener(input));

    // BatchSize value
    Label wlBatchSize = new Label(wSettingsGroup, SWT.RIGHT);
    wlBatchSize.setText(BaseMessages.getString(PKG, "SalesforceDeleteDialog.Limit.Label"));
    PropsUi.setLook(wlBatchSize);
    FormData fdlBatchSize = new FormData();
    fdlBatchSize.left = new FormAttachment(0, 0);
    fdlBatchSize.top = new FormAttachment(wRollbackAllChangesOnError, margin);
    fdlBatchSize.right = new FormAttachment(middle, -margin);
    wlBatchSize.setLayoutData(fdlBatchSize);
    wBatchSize = new TextVar(variables, wSettingsGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wBatchSize);
    wBatchSize.addModifyListener(lsMod);
    FormData fdBatchSize = new FormData();
    fdBatchSize.left = new FormAttachment(middle, 0);
    fdBatchSize.top = new FormAttachment(wRollbackAllChangesOnError, margin);
    fdBatchSize.right = new FormAttachment(100, 0);
    wBatchSize.setLayoutData(fdBatchSize);

    // Module
    Label wlModule = new Label(wSettingsGroup, SWT.RIGHT);
    wlModule.setText(BaseMessages.getString(PKG, "SalesforceDeleteDialog.Module.Label"));
    PropsUi.setLook(wlModule);
    FormData fdlModule = new FormData();
    fdlModule.left = new FormAttachment(0, 0);
    fdlModule.top = new FormAttachment(wBatchSize, margin);
    fdlModule.right = new FormAttachment(middle, -margin);
    wlModule.setLayoutData(fdlModule);
    wModule = new ComboVar(variables, wSettingsGroup, SWT.SINGLE | SWT.READ_ONLY | SWT.BORDER);
    wModule.setEditable(true);
    PropsUi.setLook(wModule);
    wModule.addModifyListener(lsTableMod);
    FormData fdModule = new FormData();
    fdModule.left = new FormAttachment(middle, 0);
    fdModule.top = new FormAttachment(wBatchSize, margin);
    fdModule.right = new FormAttachment(100, -margin);
    wModule.setLayoutData(fdModule);
    wModule.addFocusListener(
        new FocusListener() {
          @Override
          public void focusLost(org.eclipse.swt.events.FocusEvent e) {
            getModulesListError = false;
          }

          @Override
          public void focusGained(org.eclipse.swt.events.FocusEvent e) {
            // check if the URL and login credentials passed and not just had error
            if (Utils.isEmpty(wURL.getText())
                || Utils.isEmpty(wUserName.getText())
                || Utils.isEmpty(wPassword.getText())
                || (getModulesListError)) {
              return;
            }

            Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
            shell.setCursor(busy);
            getModulesList();
            shell.setCursor(null);
            busy.dispose();
          }
        });

    // Salesforce Id Field
    Label wlDeleteField = new Label(wSettingsGroup, SWT.RIGHT);
    wlDeleteField.setText(BaseMessages.getString(PKG, "SalesforceDeleteDialog.KeyField.Label"));
    PropsUi.setLook(wlDeleteField);
    FormData fdlDeleteField = new FormData();
    fdlDeleteField.left = new FormAttachment(0, 0);
    fdlDeleteField.top = new FormAttachment(wModule, margin);
    fdlDeleteField.right = new FormAttachment(middle, -margin);
    wlDeleteField.setLayoutData(fdlDeleteField);
    wDeleteField = new ComboVar(variables, wSettingsGroup, SWT.SINGLE | SWT.READ_ONLY | SWT.BORDER);
    wDeleteField.setEditable(true);
    PropsUi.setLook(wDeleteField);
    wDeleteField.addModifyListener(lsMod);
    FormData fdDeleteField = new FormData();
    fdDeleteField.left = new FormAttachment(middle, 0);
    fdDeleteField.top = new FormAttachment(wModule, margin);
    fdDeleteField.right = new FormAttachment(100, -margin);
    wDeleteField.setLayoutData(fdDeleteField);
    wDeleteField.addFocusListener(
        new FocusListener() {
          @Override
          public void focusLost(org.eclipse.swt.events.FocusEvent e) {
            // Do nothing
          }

          @Override
          public void focusGained(org.eclipse.swt.events.FocusEvent e) {
            getPreviousFields();
          }
        });

    FormData fdSettingsGroup = new FormData();
    fdSettingsGroup.left = new FormAttachment(0, margin);
    fdSettingsGroup.top = new FormAttachment(wConnectionGroup, margin);
    fdSettingsGroup.right = new FormAttachment(100, -margin);
    wSettingsGroup.setLayoutData(fdSettingsGroup);

    // ///////////////////////////////
    // END OF Settings GROUP //
    // ///////////////////////////////

    FormData fdGeneralComp = new FormData();
    fdGeneralComp.left = new FormAttachment(0, 0);
    fdGeneralComp.top = new FormAttachment(wTransformName, margin);
    fdGeneralComp.right = new FormAttachment(100, 0);
    fdGeneralComp.bottom = new FormAttachment(100, 0);
    wGeneralComp.setLayoutData(fdGeneralComp);

    wGeneralComp.layout();
    wGeneralTab.setControl(wGeneralComp);

    // THE BUTTONS
    wOk = new Button(shell, SWT.PUSH);
    wOk.setText(BaseMessages.getString(PKG, "System.Button.OK"));
    wCancel = new Button(shell, SWT.PUSH);
    wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));

    setButtonPositions(new Button[] {wOk, wCancel}, margin, null);

    FormData fdTabFolder = new FormData();
    fdTabFolder.left = new FormAttachment(0, 0);
    fdTabFolder.top = new FormAttachment(wTransformName, margin);
    fdTabFolder.right = new FormAttachment(100, 0);
    fdTabFolder.bottom = new FormAttachment(wOk, -margin);
    wTabFolder.setLayoutData(fdTabFolder);

    // Add listeners

    Listener lsTest = e -> test();

    wOk.addListener(SWT.Selection, e -> ok());
    wTest.addListener(SWT.Selection, lsTest);
    wCancel.addListener(SWT.Selection, e -> cancel());

    wTabFolder.setSelection(0);
    getData(input);
    input.setChanged(changed);

    BaseDialog.defaultShellHandling(shell, c -> ok(), c -> cancel());

    return transformName;
  }

  private void getPreviousFields() {
    if (!gotPrevious) {
      try {
        IRowMeta r = pipelineMeta.getPrevTransformFields(variables, transformName);
        if (r != null) {
          String value = wDeleteField.getText();
          wDeleteField.removeAll();
          wDeleteField.setItems(r.getFieldNames());
          if (value != null) {
            wDeleteField.setText(value);
          }
        }
      } catch (HopException ke) {
        new ErrorDialog(
            shell,
            BaseMessages.getString(PKG, "SalesforceDeleteDialog.FailedToGetFields.DialogTitle"),
            BaseMessages.getString(PKG, "SalesforceDeleteDialog.FailedToGetFields.DialogMessage"),
            ke);
      }
      gotPrevious = true;
    }
  }

  /**
   * Read the data from the TextFileInputMeta object and show it in this dialog.
   *
   * @param in The SalesforceDeleteMeta object to obtain the data from.
   */
  public void getData(SalesforceDeleteMeta in) {
    wURL.setText(Const.NVL(in.getTargetUrl(), ""));
    wUserName.setText(Const.NVL(in.getUsername(), ""));
    wPassword.setText(Const.NVL(in.getPassword(), ""));
    wBatchSize.setText(in.getBatchSize());
    wModule.setText(Const.NVL(in.getModule(), "Account"));
    if (in.getDeleteField() != null) {
      wDeleteField.setText(in.getDeleteField());
    }
    wBatchSize.setText("" + in.getBatchSize());
    wTimeOut.setText(Const.NVL(in.getTimeout(), SalesforceConnectionUtils.DEFAULT_TIMEOUT));
    wUseCompression.setSelection(in.isCompression());
    wRollbackAllChangesOnError.setSelection(in.isRollbackAllChangesOnError());

    wTransformName.selectAll();
    wTransformName.setFocus();
  }

  private void cancel() {
    transformName = null;
    input.setChanged(changed);
    dispose();
  }

  private void ok() {
    try {
      getInfo(input);
    } catch (HopException e) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(PKG, "SalesforceDeleteDialog.ErrorValidateData.DialogTitle"),
          BaseMessages.getString(PKG, "SalesforceDeleteDialog.ErrorValidateData.DialogMessage"),
          e);
    }
    dispose();
  }

  @Override
  protected void getInfo(SalesforceTransformMeta in) throws HopException {
    SalesforceDeleteMeta meta = (SalesforceDeleteMeta) in;
    transformName = wTransformName.getText(); // return value

    // copy info to SalesforceDeleteMeta class (input)
    meta.setTargetUrl(Const.NVL(wURL.getText(), SalesforceConnectionUtils.TARGET_DEFAULT_URL));
    meta.setUsername(wUserName.getText());
    meta.setPassword(wPassword.getText());
    meta.setModule(wModule.getText());
    meta.setDeleteField(wDeleteField.getText());
    meta.setBatchSize(wBatchSize.getText());
    meta.setCompression(wUseCompression.getSelection());
    meta.setTimeout(Const.NVL(wTimeOut.getText(), "0"));
    meta.setRollbackAllChangesOnError(wRollbackAllChangesOnError.getSelection());
  }

  private void getModulesList() {
    if (!gotModule) {
      SalesforceConnection connection = null;

      try {
        SalesforceDeleteMeta meta = new SalesforceDeleteMeta();
        getInfo(meta);
        String url = variables.resolve(meta.getTargetUrl());

        String selectedField = wModule.getText();
        wModule.removeAll();

        // Define a new Salesforce connection
        connection =
            new SalesforceConnection(
                log,
                url,
                variables.resolve(meta.getUsername()),
                Utils.resolvePassword(variables, meta.getPassword()));
        int realTimeOut = Const.toInt(variables.resolve(meta.getTimeout()), 0);
        connection.setTimeOut(realTimeOut);
        // connect to Salesforce
        connection.connect();
        // return
        wModule.setItems(connection.getAllAvailableObjects(false));

        if (!Utils.isEmpty(selectedField)) {
          wModule.setText(selectedField);
        }

        gotModule = true;
        getModulesListError = false;

      } catch (Exception e) {
        new ErrorDialog(
            shell,
            BaseMessages.getString(PKG, "SalesforceDeleteDialog.ErrorRetrieveModules.DialogTitle"),
            BaseMessages.getString(
                PKG, "SalesforceDeleteDialog.ErrorRetrieveData.ErrorRetrieveModules"),
            e);
        getModulesListError = true;
      } finally {
        if (connection != null) {
          try {
            connection.close();
          } catch (Exception e) {
            /* Ignore */
          }
        }
      }
    }
  }
}
