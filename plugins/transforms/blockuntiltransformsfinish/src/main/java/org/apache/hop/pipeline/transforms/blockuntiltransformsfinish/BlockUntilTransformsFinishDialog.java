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

package org.apache.hop.pipeline.transforms.blockuntiltransformsfinish;

import java.util.ArrayList;
import java.util.List;
import org.apache.hop.core.Const;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.dialog.BaseDialog;
import org.apache.hop.ui.core.widget.ColumnInfo;
import org.apache.hop.ui.core.widget.TableView;
import org.apache.hop.ui.pipeline.transform.BaseTransformDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class BlockUntilTransformsFinishDialog extends BaseTransformDialog {
  private static final Class<?> PKG = BlockUntilTransformsFinishMeta.class;

  private String[] previousTransforms;
  private final BlockUntilTransformsFinishMeta input;

  private TableView wFields;

  public BlockUntilTransformsFinishDialog(
      Shell parent,
      IVariables variables,
      BlockUntilTransformsFinishMeta transformMeta,
      PipelineMeta pipelineMeta) {
    super(parent, variables, transformMeta, pipelineMeta);
    input = transformMeta;
  }

  @Override
  public String open() {
    Shell parent = getParent();

    shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
    PropsUi.setLook(shell);
    setShellImage(shell, input);

    ModifyListener lsMod = e -> input.setChanged();
    changed = input.hasChanged();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = PropsUi.getFormMargin();
    formLayout.marginHeight = PropsUi.getFormMargin();

    shell.setLayout(formLayout);
    shell.setText(BaseMessages.getString(PKG, "BlockUntilTransformsFinishDialog.Shell.Title"));

    int middle = props.getMiddlePct();
    int margin = PropsUi.getMargin();

    // TransformName line
    wlTransformName = new Label(shell, SWT.RIGHT);
    wlTransformName.setText(
        BaseMessages.getString(PKG, "BlockUntilTransformsFinishDialog.TransformName.Label"));
    PropsUi.setLook(wlTransformName);
    fdlTransformName = new FormData();
    fdlTransformName.left = new FormAttachment(0, 0);
    fdlTransformName.right = new FormAttachment(middle, -margin);
    fdlTransformName.top = new FormAttachment(0, margin);
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

    // Get the previous transforms...
    setTransformNames();

    wOk = new Button(shell, SWT.PUSH);
    wOk.setText(BaseMessages.getString(PKG, "System.Button.OK"));
    wGet = new Button(shell, SWT.PUSH);
    wGet.setText(
        BaseMessages.getString(PKG, "BlockUntilTransformsFinishDialog.getTransforms.Label"));
    wCancel = new Button(shell, SWT.PUSH);
    wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));

    setButtonPositions(new Button[] {wOk, wGet, wCancel}, margin, null);

    // Table with fields
    Label wlFields = new Label(shell, SWT.NONE);
    wlFields.setText(BaseMessages.getString(PKG, "BlockUntilTransformsFinishDialog.Fields.Label"));
    PropsUi.setLook(wlFields);
    FormData fdlFields = new FormData();
    fdlFields.left = new FormAttachment(0, 0);
    fdlFields.top = new FormAttachment(wTransformName, margin);
    wlFields.setLayoutData(fdlFields);

    final int FieldsCols = 2;
    final int FieldsRows = input.getBlockingTransforms().size();

    ColumnInfo[] colinf = new ColumnInfo[FieldsCols];
    colinf[0] =
        new ColumnInfo(
            BaseMessages.getString(PKG, "BlockUntilTransformsFinishDialog.Fieldname.transform"),
            ColumnInfo.COLUMN_TYPE_CCOMBO,
            previousTransforms,
            false);
    colinf[1] =
        new ColumnInfo(
            BaseMessages.getString(PKG, "BlockUntilTransformsFinishDialog.Fieldname.CopyNr"),
            ColumnInfo.COLUMN_TYPE_TEXT,
            false);
    colinf[1].setUsingVariables(true);
    wFields =
        new TableView(
            variables,
            shell,
            SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI,
            colinf,
            FieldsRows,
            lsMod,
            props);

    FormData fdFields = new FormData();
    fdFields.left = new FormAttachment(0, 0);
    fdFields.top = new FormAttachment(wlFields, margin);
    fdFields.right = new FormAttachment(100, 0);
    fdFields.bottom = new FormAttachment(wOk, -2 * margin);
    wFields.setLayoutData(fdFields);

    // Add listeners
    wCancel.addListener(SWT.Selection, e -> cancel());
    wOk.addListener(SWT.Selection, e -> ok());
    wGet.addListener(SWT.Selection, e -> get());

    getData();

    BaseDialog.defaultShellHandling(shell, c -> ok(), c -> cancel());

    return transformName;
  }

  private void setTransformNames() {
    previousTransforms = pipelineMeta.getTransformNames();
    List<String> nextTransforms = getNextTransforms(new ArrayList<>(), transformMeta);

    List<String> entries = new ArrayList<>();
    for (String previousTransform : previousTransforms) {
      if (!previousTransform.equals(transformName)) {
        if (nextTransforms != null && !nextTransforms.isEmpty()) {
          boolean found = false;
          for (String nextTransform : nextTransforms) {
            if (nextTransform.equals(previousTransform)) {
              found = true;
            }
          }
          if (!found) {
            entries.add(previousTransform);
          }
        }
      }
    }
    previousTransforms = entries.toArray(new String[0]);
  }

  private List<String> getNextTransforms(List<String> transformNames, TransformMeta transformMeta) {
    List<TransformMeta> nextTransformMeta = pipelineMeta.findNextTransforms(transformMeta);
    for (TransformMeta nextTransform : nextTransformMeta) {
      transformNames.add(nextTransform.getName());
      getNextTransforms(transformNames, nextTransform);
    }
    return transformNames.stream().distinct().toList();
  }

  private void get() {
    wFields.removeAll();
    Table table = wFields.table;

    for (int i = 0; i < previousTransforms.length; i++) {
      TableItem ti = new TableItem(table, SWT.NONE);
      ti.setText(0, "" + (i + 1));
      ti.setText(1, previousTransforms[i]);
      ti.setText(2, "0");
    }
    wFields.removeEmptyRows();
    wFields.setRowNums();
    wFields.optWidth(true);
  }

  /** Copy information from the meta-data input to the dialog fields. */
  public void getData() {
    Table table = wFields.table;
    if (!input.getBlockingTransforms().isEmpty()) {
      table.removeAll();
    }
    for (int i = 0; i < input.getBlockingTransforms().size(); i++) {
      BlockingTransform blockingTransform = input.getBlockingTransforms().get(i);
      TableItem ti = new TableItem(table, SWT.NONE);
      ti.setText(1, Const.NVL(blockingTransform.getName(), ""));
      ti.setText(2, Const.NVL(blockingTransform.getCopyNr(), ""));
    }

    wFields.removeEmptyRows();
    wFields.setRowNums();
    wFields.optWidth(true);

    wTransformName.selectAll();
    wTransformName.setFocus();
  }

  private void cancel() {
    transformName = null;
    input.setChanged(changed);
    dispose();
  }

  private void ok() {
    if (Utils.isEmpty(wTransformName.getText())) {
      return;
    }
    transformName = wTransformName.getText(); // return value

    input.getBlockingTransforms().clear();
    for (TableItem item : wFields.getNonEmptyItems()) {
      String name = item.getText(1);
      String copyNr = item.getText(2);
      input.getBlockingTransforms().add(new BlockingTransform(name, copyNr));
    }
    dispose();
  }
}
