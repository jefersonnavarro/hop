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

package org.apache.hop.pipeline.transforms.fileinput.text;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.hop.core.Const;
import org.apache.hop.core.Props;
import org.apache.hop.core.compress.CompressionInputStream;
import org.apache.hop.core.compress.CompressionProviderFactory;
import org.apache.hop.core.compress.ICompressionProvider;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopPluginException;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.file.EncodingType;
import org.apache.hop.core.fileinput.FileInputList;
import org.apache.hop.core.gui.ITextFileInputField;
import org.apache.hop.core.logging.LogChannel;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.value.ValueMetaBase;
import org.apache.hop.core.row.value.ValueMetaFactory;
import org.apache.hop.core.util.EnvUtil;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.core.vfs.HopVfs;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.PipelinePreviewFactory;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.apache.hop.pipeline.transforms.common.ICsvInputAwareMeta;
import org.apache.hop.pipeline.transforms.file.BaseFileField;
import org.apache.hop.pipeline.transforms.file.BaseFileInputMeta;
import org.apache.hop.staticschema.metadata.SchemaDefinition;
import org.apache.hop.staticschema.metadata.SchemaFieldDefinition;
import org.apache.hop.staticschema.util.SchemaDefinitionUtil;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.dialog.BaseDialog;
import org.apache.hop.ui.core.dialog.EnterNumberDialog;
import org.apache.hop.ui.core.dialog.EnterNumberDialogResult;
import org.apache.hop.ui.core.dialog.EnterSelectionDialog;
import org.apache.hop.ui.core.dialog.EnterTextDialog;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.core.dialog.MessageBox;
import org.apache.hop.ui.core.dialog.PreviewRowsDialog;
import org.apache.hop.ui.core.gui.GuiResource;
import org.apache.hop.ui.core.widget.ColumnInfo;
import org.apache.hop.ui.core.widget.MetaSelectionLine;
import org.apache.hop.ui.core.widget.TableView;
import org.apache.hop.ui.core.widget.TextVar;
import org.apache.hop.ui.pipeline.dialog.PipelinePreviewProgressDialog;
import org.apache.hop.ui.pipeline.transform.BaseTransformDialog;
import org.apache.hop.ui.pipeline.transform.common.ICsvInputAwareImportProgressDialog;
import org.apache.hop.ui.pipeline.transform.common.ICsvInputAwareTransformDialog;
import org.apache.hop.ui.pipeline.transform.common.IGetFieldsCapableTransformDialog;
import org.apache.hop.ui.pipeline.transform.common.TextFileLineUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class TextFileInputDialog extends BaseTransformDialog
    implements IGetFieldsCapableTransformDialog<TextFileInputMeta>, ICsvInputAwareTransformDialog {
  private static final Class<?> PKG = TextFileInputMeta.class;

  public static final String CONST_SYSTEM_COMBO_NO = "System.Combo.No";
  public static final String CONST_SYSTEM_COMBO_YES = "System.Combo.Yes";
  private static final String[] YES_NO_COMBO =
      new String[] {
        BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_NO),
        BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_YES)
      };
  public static final String CONST_SYSTEM_DIALOG_ERROR_TITLE = "System.Dialog.Error.Title";
  public static final String CONST_SYSTEM_BUTTON_BROWSE = "System.Button.Browse";
  public static final String CONST_SYSTEM_LABEL_EXTENSION = "System.Label.Extension";

  private CTabFolder wTabFolder;

  private TextVar wExcludeFilemask;

  private Button wAccFilenames;

  private MetaSelectionLine<SchemaDefinition> wSchemaDefinition;

  private Label wlPassThruFields;
  private Button wPassThruFields;

  private Label wlAccField;
  private Text wAccField;

  private Label wlAccTransform;
  private CCombo wAccTransform;

  private Label wlFilename;
  private Button wbbFilename; // Browse: add file or directory
  private Button wbdFilename; // Delete
  private Button wbeFilename; // Edit
  private Button wbaFilename; // Add or change
  private TextVar wFilename;

  private Label wlFilenameList;
  private TableView wFilenameList;

  private Label wlFilemask;
  private TextVar wFilemask;

  private Button wbShowFiles;

  private Button wFirst;

  private Button wFirstHeader;

  private CCombo wFiletype;

  private Button wbSeparator;
  private TextVar wSeparator;

  private Text wEnclosure;

  private Text wEscape;

  private Button wHeader;

  private Button wPrependFileName;

  private Button wEnclBreaks;

  private Label wlNrHeader;
  private Text wNrHeader;

  private Button wFooter;

  private Label wlNrFooter;
  private Text wNrFooter;

  private Button wWraps;

  private Label wlNrWraps;
  private Text wNrWraps;

  private Button wLayoutPaged;

  private Label wlNrLinesPerPage;
  private Text wNrLinesPerPage;

  private Label wlNrLinesDocHeader;
  private Text wNrLinesDocHeader;

  private CCombo wCompression;

  private Button wNoempty;

  private Button wInclFilename;

  private Label wlInclFilenameField;
  private Text wInclFilenameField;

  private Button wInclRownum;

  private Label wlRownumByFileField;
  private Button wRownumByFile;

  private Label wlInclRownumField;
  private Text wInclRownumField;

  private CCombo wFormat;

  private CCombo wEncoding;

  private CCombo wLength;

  private Text wLimit;

  private Button wDateLenient;

  private CCombo wDateLocale;

  private Button wErrorIgnored;

  private Button wSkipBadFiles;

  private Label wlSkipErrorLines;
  private Button wSkipErrorLines;

  private Label wlErrorCount;
  private Text wErrorCount;

  private Label wlErrorFields;
  private Text wErrorFields;

  private Label wlErrorText;
  private Text wErrorText;

  // New entries for intelligent error handling AKA replay functionality
  // Bad files destination directory
  private Label wlWarnDestDir;
  private Button wbbWarnDestDir; // Browse: add file or directory
  private TextVar wWarnDestDir;
  private Label wlWarnExt;
  private Text wWarnExt;

  // Error messages files destination directory
  private Label wlErrorDestDir;
  private Button wbbErrorDestDir; // Browse: add file or directory
  private TextVar wErrorDestDir;
  private Label wlErrorExt;
  private Text wErrorExt;

  // Line numbers files destination directory
  private Label wlLineNrDestDir;
  private Button wbbLineNrDestDir; // Browse: add file or directory
  private TextVar wLineNrDestDir;
  private Label wlLineNrExt;
  private Text wLineNrExt;

  private TableView wFilter;

  private TableView wFields;

  private Button wAddResult;

  private final TextFileInputMeta input;

  private Button wMinWidth;

  // Wizard info...
  private Vector<ITextFileInputField> fields;

  private int middle;
  private int margin;
  private ModifyListener lsMod;

  public static final int[] dateLengths = new int[] {23, 19, 14, 10, 10, 10, 10, 8, 8, 8, 8, 6, 6};

  private boolean gotEncodings = false;

  protected boolean firstClickOnDateLocale;

  private TextVar wShortFileFieldName;
  private TextVar wPathFieldName;

  private TextVar wIsHiddenName;
  private TextVar wLastModificationTimeName;
  private TextVar wUriName;
  private TextVar wRootUriName;
  private TextVar wExtensionFieldName;
  private TextVar wSizeFieldName;

  private Text wBadFileField;

  private Text wBadFileMessageField;

  public TextFileInputDialog(
      Shell parent,
      IVariables variables,
      TextFileInputMeta transformMeta,
      PipelineMeta pipelineMeta) {
    super(parent, variables, transformMeta, pipelineMeta);
    input = transformMeta;
    firstClickOnDateLocale = true;
  }

  @Override
  public String open() {
    Shell parent = getParent();

    shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
    PropsUi.setLook(shell);
    setShellImage(shell, input);

    lsMod = e -> input.setChanged();
    changed = input.hasChanged();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = PropsUi.getFormMargin();
    formLayout.marginHeight = PropsUi.getFormMargin();

    shell.setLayout(formLayout);
    shell.setText(BaseMessages.getString(PKG, "TextFileInputDialog.DialogTitle"));

    middle = props.getMiddlePct();
    margin = PropsUi.getMargin();

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

    // Buttons at the bottom first
    //
    wOk = new Button(shell, SWT.PUSH);
    wOk.setText(BaseMessages.getString(PKG, "System.Button.OK"));
    wOk.addListener(SWT.Selection, e -> ok());
    wPreview = new Button(shell, SWT.PUSH);
    wPreview.setText(BaseMessages.getString(PKG, "TextFileInputDialog.Preview.Button"));
    wPreview.addListener(SWT.Selection, e -> preview());
    wCancel = new Button(shell, SWT.PUSH);
    wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));
    wCancel.addListener(SWT.Selection, e -> cancel());
    setButtonPositions(new Button[] {wOk, wPreview, wCancel}, margin, null);

    wTabFolder = new CTabFolder(shell, SWT.BORDER);
    PropsUi.setLook(wTabFolder, Props.WIDGET_STYLE_TAB);

    addFilesTab();
    addContentTab();
    addErrorTab();
    addFiltersTabs();
    addFieldsTabs();
    addAdditionalFieldsTab();

    FormData fdTabFolder = new FormData();
    fdTabFolder.left = new FormAttachment(0, 0);
    fdTabFolder.top = new FormAttachment(wTransformName, margin);
    fdTabFolder.right = new FormAttachment(100, 0);
    fdTabFolder.bottom = new FormAttachment(wOk, -2 * margin);
    wTabFolder.setLayoutData(fdTabFolder);

    wFirst.addListener(SWT.Selection, e -> first(false));
    wFirstHeader.addListener(SWT.Selection, e -> first(true));
    wGet.addListener(SWT.Selection, e -> get());
    wMinWidth.addListener(SWT.Selection, e -> setMinimalWidth());

    // Add the file to the list of files...
    SelectionAdapter selA =
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent arg0) {
            if (wFilename.getText() == null || wFilename.getText().isEmpty()) {
              displayErrorDialog(
                  new HopException(
                      BaseMessages.getString(
                          PKG, "TextFileInputDialog.ErrorAddingFile.ErrorMessage")),
                  "TextFileInputDialog.ErrorAddingFile.DialogMessage");
              return;
            }
            wFilenameList.add(
                wFilename.getText(),
                wFilemask.getText(),
                wExcludeFilemask.getText(),
                BaseFileInputMeta.RequiredFilesCode[0],
                BaseFileInputMeta.RequiredFilesCode[0]);
            wFilename.setText("");
            wFilemask.setText("");
            wExcludeFilemask.setText("");
            wFilenameList.removeEmptyRows();
            wFilenameList.setRowNums();
            wFilenameList.optWidth(true);
            checkCompressedFile();
          }
        };
    wbaFilename.addSelectionListener(selA);
    wFilename.addSelectionListener(selA);

    // Delete files from the list of files...
    wbdFilename.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent arg0) {
            int[] idx = wFilenameList.getSelectionIndices();
            wFilenameList.remove(idx);
            wFilenameList.removeEmptyRows();
            wFilenameList.setRowNums();
            checkCompressedFile();
          }
        });

    // Edit the selected file & remove from the list...
    wbeFilename.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent arg0) {
            int idx = wFilenameList.getSelectionIndex();
            if (idx >= 0) {
              String[] string = wFilenameList.getItem(idx);
              wFilename.setText(string[0]);
              wFilemask.setText(string[1]);
              wExcludeFilemask.setText(string[2]);
              wFilenameList.remove(idx);
            }
            wFilenameList.removeEmptyRows();
            wFilenameList.setRowNums();
          }
        });

    // Show the files that are selected at this time...
    wbShowFiles.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            showFiles();
          }
        });

    // Allow the insertion of tabs as separator...
    wbSeparator.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent se) {
            wSeparator.getTextWidget().insert("\t");
          }
        });

    SelectionAdapter lsFlags =
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            setFlags();
          }
        };

    // Enable/disable the right fields...
    wInclFilename.addSelectionListener(lsFlags);
    wInclRownum.addSelectionListener(lsFlags);
    wRownumByFile.addSelectionListener(lsFlags);
    wErrorIgnored.addSelectionListener(lsFlags);
    wSkipBadFiles.addSelectionListener(lsFlags);
    wPrependFileName.addSelectionListener(lsFlags);
    wHeader.addSelectionListener(lsFlags);
    wFooter.addSelectionListener(lsFlags);
    wWraps.addSelectionListener(lsFlags);
    wLayoutPaged.addSelectionListener(lsFlags);
    wAccFilenames.addSelectionListener(lsFlags);

    wbbFilename.addListener(
        SWT.Selection,
        e ->
            BaseDialog.presentFileDialog(
                shell,
                wFilename,
                variables,
                new String[] {"*.txt", "*.csv", "*"},
                new String[] {
                  BaseMessages.getString(PKG, "System.FileType.TextFiles"),
                  BaseMessages.getString(PKG, "System.FileType.CSVFiles"),
                  BaseMessages.getString(PKG, "System.FileType.AllFiles")
                },
                true));

    wTabFolder.setSelection(0);

    // Set the shell size, based upon previous time...
    getData(input);

    BaseDialog.defaultShellHandling(shell, c -> ok(), c -> cancel());

    return transformName;
  }

  /*check the compressed extension of the first file in the archive and change the
   * compression mode in the content tab depending on it*/
  private void checkCompressedFile() {
    if (wFilenameList.getItemCount() > 0) {
      for (int i = 0; i < wFilenameList.getItemCount(); i++) {
        String[] fileRecord = wFilenameList.getItem(i);
        String fileExtension = FilenameUtils.getExtension(fileRecord[i]);
        Collection<ICompressionProvider> compProviders =
            CompressionProviderFactory.getInstance().getCompressionProviders();
        for (ICompressionProvider provider : compProviders) {
          if (provider.getDefaultExtension() != null
              && provider.getDefaultExtension().equals(fileExtension)) {
            int toBeSelected = ArrayUtils.indexOf(wCompression.getItems(), provider.getName());
            wCompression.select(toBeSelected);
            return;
          }
        }
      }
      wCompression.select(
          ArrayUtils.indexOf(
              wCompression.getItems(),
              CompressionProviderFactory.getInstance()
                  .getCompressionProviderByName("None")
                  .getName()));
    }
  }

  private void showFiles() {
    TextFileInputMeta tfii = new TextFileInputMeta();
    getInfo(tfii, true);
    String[] files =
        FileInputList.createFilePathList(
            variables,
            tfii.inputFiles.fileName,
            tfii.inputFiles.fileMask,
            tfii.inputFiles.excludeFileMask,
            tfii.inputFiles.fileRequired,
            tfii.inputFiles.includeSubFolderBoolean());

    if (files != null && files.length > 0) {
      EnterSelectionDialog esd =
          new EnterSelectionDialog(shell, files, "Files read", "Files read:");
      esd.setViewOnly();
      esd.open();
    } else {
      MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
      mb.setMessage(BaseMessages.getString(PKG, "TextFileInputDialog.NoFilesFound.DialogMessage"));
      mb.setText(BaseMessages.getString(PKG, CONST_SYSTEM_DIALOG_ERROR_TITLE));
      mb.open();
    }
  }

  private void addFilesTab() {
    // ////////////////////////
    // START OF FILE TAB ///
    // ////////////////////////

    CTabItem wFileTab = new CTabItem(wTabFolder, SWT.NONE);
    wFileTab.setFont(GuiResource.getInstance().getFontDefault());
    wFileTab.setText(BaseMessages.getString(PKG, "TextFileInputDialog.FileTab.TabTitle"));

    ScrolledComposite wFileSComp = new ScrolledComposite(wTabFolder, SWT.V_SCROLL | SWT.H_SCROLL);
    wFileSComp.setLayout(new FillLayout());

    Composite wFileComp = new Composite(wFileSComp, SWT.NONE);
    PropsUi.setLook(wFileComp);

    FormLayout fileLayout = new FormLayout();
    fileLayout.marginWidth = 3;
    fileLayout.marginHeight = 3;
    wFileComp.setLayout(fileLayout);

    // Filename line
    wlFilename = new Label(wFileComp, SWT.RIGHT);
    wlFilename.setText(BaseMessages.getString(PKG, "TextFileInputDialog.Filename.Label"));
    PropsUi.setLook(wlFilename);
    FormData fdlFilename = new FormData();
    fdlFilename.left = new FormAttachment(0, 0);
    fdlFilename.top = new FormAttachment(0, 0);
    fdlFilename.right = new FormAttachment(middle, -margin);
    wlFilename.setLayoutData(fdlFilename);

    wbbFilename = new Button(wFileComp, SWT.PUSH | SWT.CENTER);
    PropsUi.setLook(wbbFilename);
    wbbFilename.setText(BaseMessages.getString(PKG, CONST_SYSTEM_BUTTON_BROWSE));
    wbbFilename.setToolTipText(
        BaseMessages.getString(PKG, "System.Tooltip.BrowseForFileOrDirAndAdd"));
    FormData fdbFilename = new FormData();
    fdbFilename.right = new FormAttachment(100, 0);
    fdbFilename.top = new FormAttachment(0, 0);
    wbbFilename.setLayoutData(fdbFilename);

    wbaFilename = new Button(wFileComp, SWT.PUSH | SWT.CENTER);
    PropsUi.setLook(wbaFilename);
    wbaFilename.setText(BaseMessages.getString(PKG, "TextFileInputDialog.FilenameAdd.Button"));
    wbaFilename.setToolTipText(
        BaseMessages.getString(PKG, "TextFileInputDialog.FilenameAdd.Tooltip"));
    FormData fdbaFilename = new FormData();
    fdbaFilename.right = new FormAttachment(wbbFilename, -margin);
    fdbaFilename.top = new FormAttachment(0, 0);
    wbaFilename.setLayoutData(fdbaFilename);

    wFilename = new TextVar(variables, wFileComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wFilename);
    wFilename.addModifyListener(lsMod);
    FormData fdFilename = new FormData();
    fdFilename.left = new FormAttachment(middle, 0);
    fdFilename.right = new FormAttachment(wbaFilename, -margin);
    fdFilename.top = new FormAttachment(0, 0);
    wFilename.setLayoutData(fdFilename);

    wlFilemask = new Label(wFileComp, SWT.RIGHT);
    wlFilemask.setText(BaseMessages.getString(PKG, "TextFileInputDialog.Filemask.Label"));
    PropsUi.setLook(wlFilemask);
    FormData fdlFilemask = new FormData();
    fdlFilemask.left = new FormAttachment(0, 0);
    fdlFilemask.top = new FormAttachment(wFilename, margin);
    fdlFilemask.right = new FormAttachment(middle, -margin);
    wlFilemask.setLayoutData(fdlFilemask);

    wFilemask = new TextVar(variables, wFileComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);

    PropsUi.setLook(wFilemask);
    wFilemask.addModifyListener(lsMod);
    FormData fdFilemask = new FormData();
    fdFilemask.left = new FormAttachment(middle, 0);
    fdFilemask.top = new FormAttachment(wFilename, margin);
    fdFilemask.right = new FormAttachment(wbaFilename, -margin);
    wFilemask.setLayoutData(fdFilemask);

    Label wlExcludeFilemask = new Label(wFileComp, SWT.RIGHT);
    wlExcludeFilemask.setText(
        BaseMessages.getString(PKG, "TextFileInputDialog.ExcludeFilemask.Label"));
    PropsUi.setLook(wlExcludeFilemask);
    FormData fdlExcludeFilemask = new FormData();
    fdlExcludeFilemask.left = new FormAttachment(0, 0);
    fdlExcludeFilemask.top = new FormAttachment(wFilemask, margin);
    fdlExcludeFilemask.right = new FormAttachment(middle, -margin);
    wlExcludeFilemask.setLayoutData(fdlExcludeFilemask);
    wExcludeFilemask = new TextVar(variables, wFileComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wExcludeFilemask);
    wExcludeFilemask.addModifyListener(lsMod);
    FormData fdExcludeFilemask = new FormData();
    fdExcludeFilemask.left = new FormAttachment(middle, 0);
    fdExcludeFilemask.top = new FormAttachment(wFilemask, margin);
    fdExcludeFilemask.right = new FormAttachment(wFilename, 0, SWT.RIGHT);
    wExcludeFilemask.setLayoutData(fdExcludeFilemask);

    // Filename list line
    wlFilenameList = new Label(wFileComp, SWT.RIGHT);
    wlFilenameList.setText(BaseMessages.getString(PKG, "TextFileInputDialog.FilenameList.Label"));
    PropsUi.setLook(wlFilenameList);
    FormData fdlFilenameList = new FormData();
    fdlFilenameList.left = new FormAttachment(0, 0);
    fdlFilenameList.top = new FormAttachment(wExcludeFilemask, margin);
    fdlFilenameList.right = new FormAttachment(middle, -margin);
    wlFilenameList.setLayoutData(fdlFilenameList);

    // Buttons to the right of the screen...
    wbdFilename = new Button(wFileComp, SWT.PUSH | SWT.CENTER);
    PropsUi.setLook(wbdFilename);
    wbdFilename.setText(BaseMessages.getString(PKG, "TextFileInputDialog.FilenameDelete.Button"));
    wbdFilename.setToolTipText(
        BaseMessages.getString(PKG, "TextFileInputDialog.FilenameDelete.Tooltip"));
    FormData fdbdFilename = new FormData();
    fdbdFilename.right = new FormAttachment(100, 0);
    fdbdFilename.top = new FormAttachment(wExcludeFilemask, margin);
    wbdFilename.setLayoutData(fdbdFilename);

    wbeFilename = new Button(wFileComp, SWT.PUSH | SWT.CENTER);
    PropsUi.setLook(wbeFilename);
    wbeFilename.setText(BaseMessages.getString(PKG, "TextFileInputDialog.FilenameEdit.Button"));
    wbeFilename.setToolTipText(
        BaseMessages.getString(PKG, "TextFileInputDialog.FilenameEdit.Tooltip"));
    FormData fdbeFilename = new FormData();
    fdbeFilename.right = new FormAttachment(100, 0);
    fdbeFilename.left = new FormAttachment(wbdFilename, 0, SWT.LEFT);
    fdbeFilename.top = new FormAttachment(wbdFilename, margin);
    wbeFilename.setLayoutData(fdbeFilename);

    wbShowFiles = new Button(wFileComp, SWT.PUSH | SWT.CENTER);
    PropsUi.setLook(wbShowFiles);
    wbShowFiles.setText(BaseMessages.getString(PKG, "TextFileInputDialog.ShowFiles.Button"));
    FormData fdbShowFiles = new FormData();
    fdbShowFiles.left = new FormAttachment(middle, 0);
    fdbShowFiles.bottom = new FormAttachment(100, 0);
    wbShowFiles.setLayoutData(fdbShowFiles);

    wFirst = new Button(wFileComp, SWT.PUSH);
    PropsUi.setLook(wFirst);
    wFirst.setText(BaseMessages.getString(PKG, "TextFileInputDialog.First.Button"));
    FormData fdFirst = new FormData();
    fdFirst.left = new FormAttachment(wbShowFiles, margin * 2);
    fdFirst.bottom = new FormAttachment(100, 0);
    wFirst.setLayoutData(fdFirst);

    wFirstHeader = new Button(wFileComp, SWT.PUSH);
    PropsUi.setLook(wFirstHeader);
    wFirstHeader.setText(BaseMessages.getString(PKG, "TextFileInputDialog.FirstHeader.Button"));
    FormData fdFirstHeader = new FormData();
    fdFirstHeader.left = new FormAttachment(wFirst, margin * 2);
    fdFirstHeader.bottom = new FormAttachment(100, 0);
    wFirstHeader.setLayoutData(fdFirstHeader);

    // Accepting filenames group
    //

    Group gAccepting = new Group(wFileComp, SWT.SHADOW_ETCHED_IN);
    gAccepting.setText(BaseMessages.getString(PKG, "TextFileInputDialog.AcceptingGroup.Label"));
    FormLayout acceptingLayout = new FormLayout();
    acceptingLayout.marginWidth = 3;
    acceptingLayout.marginHeight = 3;
    gAccepting.setLayout(acceptingLayout);
    PropsUi.setLook(gAccepting);

    // Accept filenames from previous transforms?
    //
    Label wlAccFilenames = new Label(gAccepting, SWT.RIGHT);
    wlAccFilenames.setText(
        BaseMessages.getString(PKG, "TextFileInputDialog.AcceptFilenames.Label"));
    PropsUi.setLook(wlAccFilenames);
    FormData fdlAccFilenames = new FormData();
    fdlAccFilenames.top = new FormAttachment(0, margin);
    fdlAccFilenames.left = new FormAttachment(0, 0);
    fdlAccFilenames.right = new FormAttachment(middle, -margin);
    wlAccFilenames.setLayoutData(fdlAccFilenames);
    wAccFilenames = new Button(gAccepting, SWT.CHECK);
    wAccFilenames.setToolTipText(
        BaseMessages.getString(PKG, "TextFileInputDialog.AcceptFilenames.Tooltip"));
    PropsUi.setLook(wAccFilenames);
    FormData fdAccFilenames = new FormData();
    fdAccFilenames.top = new FormAttachment(wlAccFilenames, 0, SWT.CENTER);
    fdAccFilenames.left = new FormAttachment(middle, 0);
    fdAccFilenames.right = new FormAttachment(100, 0);
    wAccFilenames.setLayoutData(fdAccFilenames);

    // Accept filenames from previous transforms?
    //
    wlPassThruFields = new Label(gAccepting, SWT.RIGHT);
    wlPassThruFields.setText(
        BaseMessages.getString(PKG, "TextFileInputDialog.PassThruFields.Label"));
    PropsUi.setLook(wlPassThruFields);
    FormData fdlPassThruFields = new FormData();
    fdlPassThruFields.top = new FormAttachment(wAccFilenames, margin);
    fdlPassThruFields.left = new FormAttachment(0, 0);
    fdlPassThruFields.right = new FormAttachment(middle, -margin);
    wlPassThruFields.setLayoutData(fdlPassThruFields);
    wPassThruFields = new Button(gAccepting, SWT.CHECK);
    wPassThruFields.setToolTipText(
        BaseMessages.getString(PKG, "TextFileInputDialog.PassThruFields.Tooltip"));
    PropsUi.setLook(wPassThruFields);
    FormData fdPassThruFields = new FormData();
    fdPassThruFields.top = new FormAttachment(wlPassThruFields, 0, SWT.CENTER);
    fdPassThruFields.left = new FormAttachment(middle, 0);
    fdPassThruFields.right = new FormAttachment(100, 0);
    wPassThruFields.setLayoutData(fdPassThruFields);

    // Which transform to read from?
    wlAccTransform = new Label(gAccepting, SWT.RIGHT);
    wlAccTransform.setText(
        BaseMessages.getString(PKG, "TextFileInputDialog.AcceptTransform.Label"));
    PropsUi.setLook(wlAccTransform);
    FormData fdlAccTransform = new FormData();
    fdlAccTransform.top = new FormAttachment(wPassThruFields, margin);
    fdlAccTransform.left = new FormAttachment(0, 0);
    fdlAccTransform.right = new FormAttachment(middle, -margin);
    wlAccTransform.setLayoutData(fdlAccTransform);
    wAccTransform = new CCombo(gAccepting, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wAccTransform.setToolTipText(
        BaseMessages.getString(PKG, "TextFileInputDialog.AcceptTransform.Tooltip"));
    PropsUi.setLook(wAccTransform);
    FormData fdAccTransform = new FormData();
    fdAccTransform.top = new FormAttachment(wPassThruFields, margin);
    fdAccTransform.left = new FormAttachment(middle, 0);
    fdAccTransform.right = new FormAttachment(100, 0);
    wAccTransform.setLayoutData(fdAccTransform);

    // Which field?
    //
    wlAccField = new Label(gAccepting, SWT.RIGHT);
    wlAccField.setText(BaseMessages.getString(PKG, "TextFileInputDialog.AcceptField.Label"));
    PropsUi.setLook(wlAccField);
    FormData fdlAccField = new FormData();
    fdlAccField.top = new FormAttachment(wAccTransform, margin);
    fdlAccField.left = new FormAttachment(0, 0);
    fdlAccField.right = new FormAttachment(middle, -margin);
    wlAccField.setLayoutData(fdlAccField);
    wAccField = new Text(gAccepting, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wAccField.setToolTipText(
        BaseMessages.getString(PKG, "TextFileInputDialog.AcceptField.Tooltip"));
    PropsUi.setLook(wAccField);
    FormData fdAccField = new FormData();
    fdAccField.top = new FormAttachment(wAccTransform, margin);
    fdAccField.left = new FormAttachment(middle, 0);
    fdAccField.right = new FormAttachment(100, 0);
    wAccField.setLayoutData(fdAccField);

    // Fill in the source transforms...
    List<TransformMeta> prevTransforms =
        pipelineMeta.findPreviousTransforms(pipelineMeta.findTransform(transformName));
    for (TransformMeta prevTransform : prevTransforms) {
      wAccTransform.add(prevTransform.getName());
    }

    FormData fdAccepting = new FormData();
    fdAccepting.left = new FormAttachment(0, 0);
    fdAccepting.right = new FormAttachment(100, 0);
    fdAccepting.bottom = new FormAttachment(wFirstHeader, -margin * 2);
    gAccepting.setLayoutData(fdAccepting);

    ColumnInfo[] colinfo =
        new ColumnInfo[] {
          new ColumnInfo(
              BaseMessages.getString(PKG, "TextFileInputDialog.FileDirColumn.Column"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "TextFileInputDialog.WildcardColumn.Column"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "TextFileInputDialog.Files.ExcludeWildcard.Column"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "TextFileInputDialog.RequiredColumn.Column"),
              ColumnInfo.COLUMN_TYPE_CCOMBO,
              YES_NO_COMBO),
          new ColumnInfo(
              BaseMessages.getString(PKG, "TextFileInputDialog.IncludeSubDirs.Column"),
              ColumnInfo.COLUMN_TYPE_CCOMBO,
              YES_NO_COMBO)
        };

    colinfo[0].setUsingVariables(true);
    colinfo[1].setToolTip(BaseMessages.getString(PKG, "TextFileInputDialog.RegExpColumn.Column"));

    colinfo[1].setUsingVariables(true);

    colinfo[2].setUsingVariables(true);
    colinfo[2].setToolTip(
        BaseMessages.getString(PKG, "TextFileInputDialog.Files.ExcludeWildcard.Tooltip"));
    colinfo[3].setToolTip(
        BaseMessages.getString(PKG, "TextFileInputDialog.RequiredColumn.Tooltip"));
    colinfo[4].setToolTip(
        BaseMessages.getString(PKG, "TextFileInputDialog.IncludeSubDirs.Tooltip"));

    wFilenameList =
        new TableView(
            variables,
            wFileComp,
            SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER,
            colinfo,
            4,
            lsMod,
            props);
    PropsUi.setLook(wFilenameList);
    FormData fdFilenameList = new FormData();
    fdFilenameList.left = new FormAttachment(middle, 0);
    fdFilenameList.right = new FormAttachment(wbdFilename, -margin);
    fdFilenameList.top = new FormAttachment(wExcludeFilemask, margin);
    fdFilenameList.bottom = new FormAttachment(gAccepting, -margin);
    wFilenameList.setLayoutData(fdFilenameList);

    FormData fdFileComp = new FormData();
    fdFileComp.left = new FormAttachment(0, 0);
    fdFileComp.top = new FormAttachment(0, 0);
    fdFileComp.right = new FormAttachment(100, 0);
    fdFileComp.bottom = new FormAttachment(100, 0);
    wFileComp.setLayoutData(fdFileComp);

    wFileComp.pack();
    Rectangle bounds = wFileComp.getBounds();

    wFileSComp.setContent(wFileComp);
    wFileSComp.setExpandHorizontal(true);
    wFileSComp.setExpandVertical(true);
    wFileSComp.setMinWidth(bounds.width);
    wFileSComp.setMinHeight(bounds.height);

    wFileTab.setControl(wFileSComp);

    // ///////////////////////////////////////////////////////////
    // / END OF FILE TAB
    // ///////////////////////////////////////////////////////////
  }

  private void addContentTab() {
    // ////////////////////////
    // START OF CONTENT TAB///
    // /
    CTabItem wContentTab = new CTabItem(wTabFolder, SWT.NONE);
    wContentTab.setFont(GuiResource.getInstance().getFontDefault());
    wContentTab.setText(BaseMessages.getString(PKG, "TextFileInputDialog.ContentTab.TabTitle"));

    FormLayout contentLayout = new FormLayout();
    contentLayout.marginWidth = 3;
    contentLayout.marginHeight = 3;

    ScrolledComposite wContentSComp =
        new ScrolledComposite(wTabFolder, SWT.V_SCROLL | SWT.H_SCROLL);
    wContentSComp.setLayout(new FillLayout());

    Composite wContentComp = new Composite(wContentSComp, SWT.NONE);
    PropsUi.setLook(wContentComp);
    wContentComp.setLayout(contentLayout);

    // Filetype line
    Label wlFiletype = new Label(wContentComp, SWT.RIGHT);
    wlFiletype.setText(BaseMessages.getString(PKG, "TextFileInputDialog.Filetype.Label"));
    PropsUi.setLook(wlFiletype);
    FormData fdlFiletype = new FormData();
    fdlFiletype.left = new FormAttachment(0, 0);
    fdlFiletype.top = new FormAttachment(0, 0);
    fdlFiletype.right = new FormAttachment(middle, -margin);
    wlFiletype.setLayoutData(fdlFiletype);
    wFiletype = new CCombo(wContentComp, SWT.BORDER | SWT.READ_ONLY);
    wFiletype.setText(BaseMessages.getString(PKG, "TextFileInputDialog.Filetype.Label"));
    PropsUi.setLook(wFiletype);
    wFiletype.add("CSV");
    wFiletype.add("Fixed");
    wFiletype.select(0);
    wFiletype.addModifyListener(lsMod);
    FormData fdFiletype = new FormData();
    fdFiletype.left = new FormAttachment(middle, 0);
    fdFiletype.top = new FormAttachment(0, 0);
    fdFiletype.right = new FormAttachment(100, 0);
    wFiletype.setLayoutData(fdFiletype);

    Label wlSeparator = new Label(wContentComp, SWT.RIGHT);
    wlSeparator.setText(BaseMessages.getString(PKG, "TextFileInputDialog.Separator.Label"));
    PropsUi.setLook(wlSeparator);
    FormData fdlSeparator = new FormData();
    fdlSeparator.left = new FormAttachment(0, 0);
    fdlSeparator.top = new FormAttachment(wFiletype, margin);
    fdlSeparator.right = new FormAttachment(middle, -margin);
    wlSeparator.setLayoutData(fdlSeparator);

    wbSeparator = new Button(wContentComp, SWT.PUSH | SWT.CENTER);
    wbSeparator.setText(BaseMessages.getString(PKG, "TextFileInputDialog.Delimiter.Button"));
    PropsUi.setLook(wbSeparator);
    FormData fdbSeparator = new FormData();
    fdbSeparator.right = new FormAttachment(100, 0);
    fdbSeparator.top = new FormAttachment(wFiletype, 0);
    wbSeparator.setLayoutData(fdbSeparator);
    wSeparator = new TextVar(variables, wContentComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wSeparator);
    wSeparator.addModifyListener(lsMod);
    FormData fdSeparator = new FormData();
    fdSeparator.top = new FormAttachment(wFiletype, margin);
    fdSeparator.left = new FormAttachment(middle, 0);
    fdSeparator.right = new FormAttachment(wbSeparator, -margin);
    wSeparator.setLayoutData(fdSeparator);

    // Enclosure
    Label wlEnclosure = new Label(wContentComp, SWT.RIGHT);
    wlEnclosure.setText(BaseMessages.getString(PKG, "TextFileInputDialog.Enclosure.Label"));
    PropsUi.setLook(wlEnclosure);
    FormData fdlEnclosure = new FormData();
    fdlEnclosure.left = new FormAttachment(0, 0);
    fdlEnclosure.top = new FormAttachment(wSeparator, margin);
    fdlEnclosure.right = new FormAttachment(middle, -margin);
    wlEnclosure.setLayoutData(fdlEnclosure);
    wEnclosure = new Text(wContentComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wEnclosure);
    wEnclosure.addModifyListener(lsMod);
    FormData fdEnclosure = new FormData();
    fdEnclosure.left = new FormAttachment(middle, 0);
    fdEnclosure.top = new FormAttachment(wSeparator, margin);
    fdEnclosure.right = new FormAttachment(100, 0);
    wEnclosure.setLayoutData(fdEnclosure);

    // Allow Enclosure breaks checkbox
    Label wlEnclBreaks = new Label(wContentComp, SWT.RIGHT);
    wlEnclBreaks.setText(BaseMessages.getString(PKG, "TextFileInputDialog.EnclBreaks.Label"));
    PropsUi.setLook(wlEnclBreaks);
    FormData fdlEnclBreaks = new FormData();
    fdlEnclBreaks.left = new FormAttachment(0, 0);
    fdlEnclBreaks.top = new FormAttachment(wEnclosure, margin);
    fdlEnclBreaks.right = new FormAttachment(middle, -margin);
    wlEnclBreaks.setLayoutData(fdlEnclBreaks);
    wEnclBreaks = new Button(wContentComp, SWT.CHECK);
    PropsUi.setLook(wEnclBreaks);
    FormData fdEnclBreaks = new FormData();
    fdEnclBreaks.left = new FormAttachment(middle, 0);
    fdEnclBreaks.top = new FormAttachment(wlEnclBreaks, 0, SWT.CENTER);
    wEnclBreaks.setLayoutData(fdEnclBreaks);

    // Escape
    Label wlEscape = new Label(wContentComp, SWT.RIGHT);
    wlEscape.setText(BaseMessages.getString(PKG, "TextFileInputDialog.Escape.Label"));
    PropsUi.setLook(wlEscape);
    FormData fdlEscape = new FormData();
    fdlEscape.left = new FormAttachment(0, 0);
    fdlEscape.top = new FormAttachment(wEnclBreaks, margin);
    fdlEscape.right = new FormAttachment(middle, -margin);
    wlEscape.setLayoutData(fdlEscape);
    wEscape = new Text(wContentComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wEscape);
    wEscape.addModifyListener(lsMod);
    FormData fdEscape = new FormData();
    fdEscape.left = new FormAttachment(middle, 0);
    fdEscape.top = new FormAttachment(wEnclBreaks, margin);
    fdEscape.right = new FormAttachment(100, 0);
    wEscape.setLayoutData(fdEscape);

    // Addition Prepend file name
    Label wlPrependFileName = new Label(wContentComp, SWT.RIGHT);
    wlPrependFileName.setText(
        BaseMessages.getString(PKG, "TextFileInputDialog.PrependFileName.Label"));
    PropsUi.setLook(wlPrependFileName);
    FormData fdlPrependFileName = new FormData();
    fdlPrependFileName.left = new FormAttachment(0, 0);
    fdlPrependFileName.top = new FormAttachment(wEscape, margin);
    fdlPrependFileName.right = new FormAttachment(middle, -margin);
    wlPrependFileName.setLayoutData(fdlPrependFileName);
    wPrependFileName = new Button(wContentComp, SWT.CHECK);
    PropsUi.setLook(wPrependFileName);
    FormData fdPrependFileName = new FormData();
    fdPrependFileName.left = new FormAttachment(middle, 0);
    fdPrependFileName.top = new FormAttachment(wlPrependFileName, 0, SWT.CENTER);
    wPrependFileName.setLayoutData(fdPrependFileName);

    // Header checkbox
    Label wlHeader = new Label(wContentComp, SWT.RIGHT);
    wlHeader.setText(BaseMessages.getString(PKG, "TextFileInputDialog.Header.Label"));
    PropsUi.setLook(wlHeader);
    FormData fdlHeader = new FormData();
    fdlHeader.left = new FormAttachment(0, 0);
    fdlHeader.top = new FormAttachment(wPrependFileName, margin);
    fdlHeader.right = new FormAttachment(middle, -margin);
    wlHeader.setLayoutData(fdlHeader);
    wHeader = new Button(wContentComp, SWT.CHECK);
    PropsUi.setLook(wHeader);
    FormData fdHeader = new FormData();
    fdHeader.left = new FormAttachment(middle, 0);
    fdHeader.top = new FormAttachment(wlHeader, 0, SWT.CENTER);
    wHeader.setLayoutData(fdHeader);

    // NrHeader
    wlNrHeader = new Label(wContentComp, SWT.RIGHT);
    wlNrHeader.setText(BaseMessages.getString(PKG, "TextFileInputDialog.NrHeader.Label"));
    PropsUi.setLook(wlNrHeader);
    FormData fdlNrHeader = new FormData();
    fdlNrHeader.left = new FormAttachment(wHeader, margin);
    fdlNrHeader.top = new FormAttachment(wlHeader, 0, SWT.CENTER);
    wlNrHeader.setLayoutData(fdlNrHeader);
    wNrHeader = new Text(wContentComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wNrHeader.setTextLimit(3);
    PropsUi.setLook(wNrHeader);
    wNrHeader.addModifyListener(lsMod);
    FormData fdNrHeader = new FormData();
    fdNrHeader.left = new FormAttachment(wlNrHeader, margin);
    fdNrHeader.top = new FormAttachment(wlHeader, 0, SWT.CENTER);
    fdNrHeader.right = new FormAttachment(100, 0);
    wNrHeader.setLayoutData(fdNrHeader);

    Label wlFooter = new Label(wContentComp, SWT.RIGHT);
    wlFooter.setText(BaseMessages.getString(PKG, "TextFileInputDialog.Footer.Label"));
    PropsUi.setLook(wlFooter);
    FormData fdlFooter = new FormData();
    fdlFooter.left = new FormAttachment(0, 0);
    fdlFooter.top = new FormAttachment(wHeader, margin);
    fdlFooter.right = new FormAttachment(middle, -margin);
    wlFooter.setLayoutData(fdlFooter);
    wFooter = new Button(wContentComp, SWT.CHECK);
    PropsUi.setLook(wFooter);
    FormData fdFooter = new FormData();
    fdFooter.left = new FormAttachment(middle, 0);
    fdFooter.top = new FormAttachment(wlFooter, 0, SWT.CENTER);
    wFooter.setLayoutData(fdFooter);

    // NrFooter
    wlNrFooter = new Label(wContentComp, SWT.RIGHT);
    wlNrFooter.setText(BaseMessages.getString(PKG, "TextFileInputDialog.NrFooter.Label"));
    PropsUi.setLook(wlNrFooter);
    FormData fdlNrFooter = new FormData();
    fdlNrFooter.left = new FormAttachment(wFooter, margin);
    fdlNrFooter.top = new FormAttachment(wlFooter, 0, SWT.CENTER);
    wlNrFooter.setLayoutData(fdlNrFooter);
    wNrFooter = new Text(wContentComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wNrFooter.setTextLimit(3);
    PropsUi.setLook(wNrFooter);
    wNrFooter.addModifyListener(lsMod);
    FormData fdNrFooter = new FormData();
    fdNrFooter.left = new FormAttachment(wlNrFooter, margin);
    fdNrFooter.top = new FormAttachment(wlFooter, 0, SWT.CENTER);
    fdNrFooter.right = new FormAttachment(100, 0);
    wNrFooter.setLayoutData(fdNrFooter);

    // Wraps
    Label wlWraps = new Label(wContentComp, SWT.RIGHT);
    wlWraps.setText(BaseMessages.getString(PKG, "TextFileInputDialog.Wraps.Label"));
    PropsUi.setLook(wlWraps);
    FormData fdlWraps = new FormData();
    fdlWraps.left = new FormAttachment(0, 0);
    fdlWraps.top = new FormAttachment(wFooter, margin);
    fdlWraps.right = new FormAttachment(middle, -margin);
    wlWraps.setLayoutData(fdlWraps);
    wWraps = new Button(wContentComp, SWT.CHECK);
    PropsUi.setLook(wWraps);
    FormData fdWraps = new FormData();
    fdWraps.left = new FormAttachment(middle, 0);
    fdWraps.top = new FormAttachment(wlWraps, 0, SWT.CENTER);
    wWraps.setLayoutData(fdWraps);

    // NrWraps
    wlNrWraps = new Label(wContentComp, SWT.RIGHT);
    wlNrWraps.setText(BaseMessages.getString(PKG, "TextFileInputDialog.NrWraps.Label"));
    PropsUi.setLook(wlNrWraps);
    FormData fdlNrWraps = new FormData();
    fdlNrWraps.left = new FormAttachment(wWraps, margin);
    fdlNrWraps.top = new FormAttachment(wlWraps, 0, SWT.CENTER);
    wlNrWraps.setLayoutData(fdlNrWraps);
    wNrWraps = new Text(wContentComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wNrWraps.setTextLimit(3);
    PropsUi.setLook(wNrWraps);
    wNrWraps.addModifyListener(lsMod);
    FormData fdNrWraps = new FormData();
    fdNrWraps.left = new FormAttachment(wlNrWraps, margin);
    fdNrWraps.top = new FormAttachment(wlWraps, 0, SWT.CENTER);
    fdNrWraps.right = new FormAttachment(100, 0);
    wNrWraps.setLayoutData(fdNrWraps);

    // Pages
    Label wlLayoutPaged = new Label(wContentComp, SWT.RIGHT);
    wlLayoutPaged.setText(BaseMessages.getString(PKG, "TextFileInputDialog.LayoutPaged.Label"));
    PropsUi.setLook(wlLayoutPaged);
    FormData fdlLayoutPaged = new FormData();
    fdlLayoutPaged.left = new FormAttachment(0, 0);
    fdlLayoutPaged.top = new FormAttachment(wWraps, margin);
    fdlLayoutPaged.right = new FormAttachment(middle, -margin);
    wlLayoutPaged.setLayoutData(fdlLayoutPaged);
    wLayoutPaged = new Button(wContentComp, SWT.CHECK);
    PropsUi.setLook(wLayoutPaged);
    FormData fdLayoutPaged = new FormData();
    fdLayoutPaged.left = new FormAttachment(middle, 0);
    fdLayoutPaged.top = new FormAttachment(wlLayoutPaged, 0, SWT.CENTER);
    wLayoutPaged.setLayoutData(fdLayoutPaged);

    // Nr of lines per page
    wlNrLinesPerPage = new Label(wContentComp, SWT.RIGHT);
    wlNrLinesPerPage.setText(
        BaseMessages.getString(PKG, "TextFileInputDialog.NrLinesPerPage.Label"));
    PropsUi.setLook(wlNrLinesPerPage);
    FormData fdlNrLinesPerPage = new FormData();
    fdlNrLinesPerPage.left = new FormAttachment(wLayoutPaged, margin);
    fdlNrLinesPerPage.top = new FormAttachment(wlLayoutPaged, 0, SWT.CENTER);
    wlNrLinesPerPage.setLayoutData(fdlNrLinesPerPage);
    wNrLinesPerPage = new Text(wContentComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wNrLinesPerPage.setTextLimit(3);
    PropsUi.setLook(wNrLinesPerPage);
    wNrLinesPerPage.addModifyListener(lsMod);
    FormData fdNrLinesPerPage = new FormData();
    fdNrLinesPerPage.left = new FormAttachment(wlNrLinesPerPage, margin);
    fdNrLinesPerPage.top = new FormAttachment(wlLayoutPaged, 0, SWT.CENTER);
    fdNrLinesPerPage.right = new FormAttachment(100, 0);
    wNrLinesPerPage.setLayoutData(fdNrLinesPerPage);

    // NrPages
    wlNrLinesDocHeader = new Label(wContentComp, SWT.RIGHT);
    wlNrLinesDocHeader.setText(
        BaseMessages.getString(PKG, "TextFileInputDialog.NrLinesDocHeader.Label"));
    PropsUi.setLook(wlNrLinesDocHeader);
    FormData fdlNrLinesDocHeader = new FormData();
    fdlNrLinesDocHeader.left = new FormAttachment(wLayoutPaged, margin);
    fdlNrLinesDocHeader.top = new FormAttachment(wNrLinesPerPage, margin);
    wlNrLinesDocHeader.setLayoutData(fdlNrLinesDocHeader);
    wNrLinesDocHeader = new Text(wContentComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wNrLinesDocHeader.setTextLimit(3);
    PropsUi.setLook(wNrLinesDocHeader);
    wNrLinesDocHeader.addModifyListener(lsMod);
    FormData fdNrLinesDocHeader = new FormData();

    fdNrLinesDocHeader.left = new FormAttachment(wlNrLinesPerPage, margin);
    fdNrLinesDocHeader.top = new FormAttachment(wNrLinesPerPage, margin);
    fdNrLinesDocHeader.right = new FormAttachment(100, 0);
    wNrLinesDocHeader.setLayoutData(fdNrLinesDocHeader);

    // Compression type (None, Zip or GZip
    Label wlCompression = new Label(wContentComp, SWT.RIGHT);
    wlCompression.setText(BaseMessages.getString(PKG, "TextFileInputDialog.Compression.Label"));
    PropsUi.setLook(wlCompression);
    FormData fdlCompression = new FormData();
    fdlCompression.left = new FormAttachment(0, 0);
    fdlCompression.top = new FormAttachment(wNrLinesDocHeader, margin);
    fdlCompression.right = new FormAttachment(middle, -margin);
    wlCompression.setLayoutData(fdlCompression);
    wCompression = new CCombo(wContentComp, SWT.BORDER | SWT.READ_ONLY);
    wCompression.setText(BaseMessages.getString(PKG, "TextFileInputDialog.Compression.Label"));
    wCompression.setToolTipText(
        BaseMessages.getString(PKG, "TextFileInputDialog.Compression.Tooltip"));
    PropsUi.setLook(wCompression);
    wCompression.setItems(CompressionProviderFactory.getInstance().getCompressionProviderNames());

    wCompression.addModifyListener(lsMod);
    FormData fdCompression = new FormData();
    fdCompression.left = new FormAttachment(middle, 0);
    fdCompression.top = new FormAttachment(wNrLinesDocHeader, margin);
    fdCompression.right = new FormAttachment(100, 0);
    wCompression.setLayoutData(fdCompression);

    Label wlNoempty = new Label(wContentComp, SWT.RIGHT);
    wlNoempty.setText(BaseMessages.getString(PKG, "TextFileInputDialog.NoEmpty.Label"));
    PropsUi.setLook(wlNoempty);
    FormData fdlNoempty = new FormData();
    fdlNoempty.left = new FormAttachment(0, 0);
    fdlNoempty.top = new FormAttachment(wCompression, margin);
    fdlNoempty.right = new FormAttachment(middle, -margin);
    wlNoempty.setLayoutData(fdlNoempty);
    wNoempty = new Button(wContentComp, SWT.CHECK);
    PropsUi.setLook(wNoempty);
    wNoempty.setToolTipText(BaseMessages.getString(PKG, "TextFileInputDialog.NoEmpty.Tooltip"));
    FormData fdNoempty = new FormData();
    fdNoempty.left = new FormAttachment(middle, 0);
    fdNoempty.top = new FormAttachment(wlNoempty, 0, SWT.CENTER);
    fdNoempty.right = new FormAttachment(100, 0);
    wNoempty.setLayoutData(fdNoempty);

    Label wlInclFilename = new Label(wContentComp, SWT.RIGHT);
    wlInclFilename.setText(BaseMessages.getString(PKG, "TextFileInputDialog.InclFilename.Label"));
    PropsUi.setLook(wlInclFilename);
    FormData fdlInclFilename = new FormData();
    fdlInclFilename.left = new FormAttachment(0, 0);
    fdlInclFilename.top = new FormAttachment(wNoempty, margin);
    fdlInclFilename.right = new FormAttachment(middle, -margin);
    wlInclFilename.setLayoutData(fdlInclFilename);
    wInclFilename = new Button(wContentComp, SWT.CHECK);
    PropsUi.setLook(wInclFilename);
    wInclFilename.setToolTipText(
        BaseMessages.getString(PKG, "TextFileInputDialog.InclFilename.Tooltip"));
    FormData fdInclFilename = new FormData();
    fdInclFilename.left = new FormAttachment(middle, 0);
    fdInclFilename.top = new FormAttachment(wlInclFilename, 0, SWT.CENTER);
    wInclFilename.setLayoutData(fdInclFilename);

    wlInclFilenameField = new Label(wContentComp, SWT.LEFT);
    wlInclFilenameField.setText(
        BaseMessages.getString(PKG, "TextFileInputDialog.InclFilenameField.Label"));
    PropsUi.setLook(wlInclFilenameField);
    FormData fdlInclFilenameField = new FormData();
    fdlInclFilenameField.left = new FormAttachment(wInclFilename, margin);
    fdlInclFilenameField.top = new FormAttachment(wlInclFilename, 0, SWT.CENTER);
    wlInclFilenameField.setLayoutData(fdlInclFilenameField);
    wInclFilenameField = new Text(wContentComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wInclFilenameField);
    wInclFilenameField.addModifyListener(lsMod);
    FormData fdInclFilenameField = new FormData();
    fdInclFilenameField.left = new FormAttachment(wlInclFilenameField, margin);
    fdInclFilenameField.top = new FormAttachment(wlInclFilename, 0, SWT.CENTER);
    fdInclFilenameField.right = new FormAttachment(100, 0);
    wInclFilenameField.setLayoutData(fdInclFilenameField);

    Label wlInclRownum = new Label(wContentComp, SWT.RIGHT);
    wlInclRownum.setText(BaseMessages.getString(PKG, "TextFileInputDialog.InclRownum.Label"));
    PropsUi.setLook(wlInclRownum);
    FormData fdlInclRownum = new FormData();
    fdlInclRownum.left = new FormAttachment(0, 0);
    fdlInclRownum.top = new FormAttachment(wInclFilenameField, margin);
    fdlInclRownum.right = new FormAttachment(middle, -margin);
    wlInclRownum.setLayoutData(fdlInclRownum);
    wInclRownum = new Button(wContentComp, SWT.CHECK);
    PropsUi.setLook(wInclRownum);
    wInclRownum.setToolTipText(
        BaseMessages.getString(PKG, "TextFileInputDialog.InclRownum.Tooltip"));
    FormData fdRownum = new FormData();
    fdRownum.left = new FormAttachment(middle, 0);
    fdRownum.top = new FormAttachment(wlInclRownum, 0, SWT.CENTER);
    wInclRownum.setLayoutData(fdRownum);

    wlInclRownumField = new Label(wContentComp, SWT.RIGHT);
    wlInclRownumField.setText(
        BaseMessages.getString(PKG, "TextFileInputDialog.InclRownumField.Label"));
    PropsUi.setLook(wlInclRownumField);
    FormData fdlInclRownumField = new FormData();
    fdlInclRownumField.left = new FormAttachment(wInclRownum, margin);
    fdlInclRownumField.top = new FormAttachment(wlInclRownum, 0, SWT.CENTER);
    wlInclRownumField.setLayoutData(fdlInclRownumField);
    wInclRownumField = new Text(wContentComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wInclRownumField);
    wInclRownumField.addModifyListener(lsMod);
    FormData fdInclRownumField = new FormData();
    fdInclRownumField.left = new FormAttachment(wlInclRownumField, margin);
    fdInclRownumField.top = new FormAttachment(wlInclRownum, 0, SWT.CENTER);
    fdInclRownumField.right = new FormAttachment(100, 0);
    wInclRownumField.setLayoutData(fdInclRownumField);

    wlRownumByFileField = new Label(wContentComp, SWT.RIGHT);
    wlRownumByFileField.setText(
        BaseMessages.getString(PKG, "TextFileInputDialog.RownumByFile.Label"));
    PropsUi.setLook(wlRownumByFileField);
    FormData fdlRownumByFile = new FormData();
    fdlRownumByFile.left = new FormAttachment(wInclRownum, margin);
    fdlRownumByFile.top = new FormAttachment(wInclRownumField, margin);
    wlRownumByFileField.setLayoutData(fdlRownumByFile);
    wRownumByFile = new Button(wContentComp, SWT.CHECK);
    PropsUi.setLook(wRownumByFile);
    wRownumByFile.setToolTipText(
        BaseMessages.getString(PKG, "TextFileInputDialog.RownumByFile.Tooltip"));
    FormData fdRownumByFile = new FormData();
    fdRownumByFile.left = new FormAttachment(wlRownumByFileField, margin);
    fdRownumByFile.top = new FormAttachment(wlRownumByFileField, 0, SWT.CENTER);
    wRownumByFile.setLayoutData(fdRownumByFile);

    Label wlFormat = new Label(wContentComp, SWT.RIGHT);
    wlFormat.setText(BaseMessages.getString(PKG, "TextFileInputDialog.Format.Label"));
    PropsUi.setLook(wlFormat);
    FormData fdlFormat = new FormData();
    fdlFormat.left = new FormAttachment(0, 0);
    fdlFormat.top = new FormAttachment(wRownumByFile, margin * 2);
    fdlFormat.right = new FormAttachment(middle, -margin);
    wlFormat.setLayoutData(fdlFormat);
    wFormat = new CCombo(wContentComp, SWT.BORDER | SWT.READ_ONLY);
    wFormat.setText(BaseMessages.getString(PKG, "TextFileInputDialog.Format.Label"));
    PropsUi.setLook(wFormat);
    wFormat.add("DOS");
    wFormat.add("Unix");
    wFormat.add("mixed");
    wFormat.select(0);
    wFormat.addModifyListener(lsMod);
    FormData fdFormat = new FormData();
    fdFormat.left = new FormAttachment(middle, 0);
    fdFormat.top = new FormAttachment(wRownumByFile, margin * 2);
    fdFormat.right = new FormAttachment(100, 0);
    wFormat.setLayoutData(fdFormat);

    Label wlEncoding = new Label(wContentComp, SWT.RIGHT);
    wlEncoding.setText(BaseMessages.getString(PKG, "TextFileInputDialog.Encoding.Label"));
    PropsUi.setLook(wlEncoding);
    FormData fdlEncoding = new FormData();
    fdlEncoding.left = new FormAttachment(0, 0);
    fdlEncoding.top = new FormAttachment(wFormat, margin);
    fdlEncoding.right = new FormAttachment(middle, -margin);
    wlEncoding.setLayoutData(fdlEncoding);
    wEncoding = new CCombo(wContentComp, SWT.BORDER | SWT.READ_ONLY);
    wEncoding.setEditable(true);
    PropsUi.setLook(wEncoding);
    wEncoding.addModifyListener(lsMod);
    FormData fdEncoding = new FormData();
    fdEncoding.left = new FormAttachment(middle, 0);
    fdEncoding.top = new FormAttachment(wFormat, margin);
    fdEncoding.right = new FormAttachment(100, 0);
    wEncoding.setLayoutData(fdEncoding);
    wEncoding.addFocusListener(
        new FocusListener() {
          @Override
          public void focusLost(FocusEvent e) {
            // Do Nothing
          }

          @Override
          public void focusGained(FocusEvent e) {
            Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
            shell.setCursor(busy);
            setEncodings();
            shell.setCursor(null);
            busy.dispose();
          }
        });

    Label wlLength = new Label(wContentComp, SWT.RIGHT);
    wlLength.setText(BaseMessages.getString(PKG, "TextFileInputDialog.Length.Label"));
    PropsUi.setLook(wlLength);
    FormData fdlLength = new FormData();
    fdlLength.left = new FormAttachment(0, 0);
    fdlLength.top = new FormAttachment(wEncoding, margin);
    fdlLength.right = new FormAttachment(middle, -margin);
    wlLength.setLayoutData(fdlLength);
    wLength = new CCombo(wContentComp, SWT.BORDER | SWT.READ_ONLY);
    wLength.setText(BaseMessages.getString(PKG, "TextFileInputDialog.Length.Label"));
    PropsUi.setLook(wLength);
    wLength.add("Characters");
    wLength.add("Bytes");
    wLength.select(0);
    wLength.addModifyListener(lsMod);
    FormData fdLength = new FormData();
    fdLength.left = new FormAttachment(middle, 0);
    fdLength.top = new FormAttachment(wEncoding, margin);
    fdLength.right = new FormAttachment(100, 0);
    wLength.setLayoutData(fdLength);

    Label wlLimit = new Label(wContentComp, SWT.RIGHT);
    wlLimit.setText(BaseMessages.getString(PKG, "TextFileInputDialog.Limit.Label"));
    PropsUi.setLook(wlLimit);
    FormData fdlLimit = new FormData();
    fdlLimit.left = new FormAttachment(0, 0);
    fdlLimit.top = new FormAttachment(wLength, margin);
    fdlLimit.right = new FormAttachment(middle, -margin);
    wlLimit.setLayoutData(fdlLimit);
    wLimit = new Text(wContentComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wLimit);
    wLimit.addModifyListener(lsMod);
    FormData fdLimit = new FormData();
    fdLimit.left = new FormAttachment(middle, 0);
    fdLimit.top = new FormAttachment(wLength, margin);
    fdLimit.right = new FormAttachment(100, 0);
    wLimit.setLayoutData(fdLimit);

    // Date Lenient checkbox
    Label wlDateLenient = new Label(wContentComp, SWT.RIGHT);
    wlDateLenient.setText(BaseMessages.getString(PKG, "TextFileInputDialog.DateLenient.Label"));
    PropsUi.setLook(wlDateLenient);
    FormData fdlDateLenient = new FormData();
    fdlDateLenient.left = new FormAttachment(0, 0);
    fdlDateLenient.top = new FormAttachment(wLimit, margin);
    fdlDateLenient.right = new FormAttachment(middle, -margin);
    wlDateLenient.setLayoutData(fdlDateLenient);
    wDateLenient = new Button(wContentComp, SWT.CHECK);
    wDateLenient.setToolTipText(
        BaseMessages.getString(PKG, "TextFileInputDialog.DateLenient.Tooltip"));
    PropsUi.setLook(wDateLenient);
    FormData fdDateLenient = new FormData();
    fdDateLenient.left = new FormAttachment(middle, 0);
    fdDateLenient.top = new FormAttachment(wlDateLenient, 0, SWT.CENTER);
    wDateLenient.setLayoutData(fdDateLenient);

    Label wlDateLocale = new Label(wContentComp, SWT.RIGHT);
    wlDateLocale.setText(BaseMessages.getString(PKG, "TextFileInputDialog.DateLocale.Label"));
    PropsUi.setLook(wlDateLocale);
    FormData fdlDateLocale = new FormData();
    fdlDateLocale.left = new FormAttachment(0, 0);
    fdlDateLocale.top = new FormAttachment(wDateLenient, margin);
    fdlDateLocale.right = new FormAttachment(middle, -margin);
    wlDateLocale.setLayoutData(fdlDateLocale);
    wDateLocale = new CCombo(wContentComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wDateLocale.setToolTipText(
        BaseMessages.getString(PKG, "TextFileInputDialog.DateLocale.Tooltip"));
    PropsUi.setLook(wDateLocale);
    wDateLocale.addModifyListener(lsMod);
    FormData fdDateLocale = new FormData();
    fdDateLocale.left = new FormAttachment(middle, 0);
    fdDateLocale.top = new FormAttachment(wDateLenient, margin);
    fdDateLocale.right = new FormAttachment(100, 0);
    wDateLocale.setLayoutData(fdDateLocale);
    wDateLocale.addFocusListener(
        new FocusListener() {
          @Override
          public void focusLost(FocusEvent e) {
            // Do Nothing
          }

          @Override
          public void focusGained(FocusEvent e) {
            Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
            shell.setCursor(busy);
            setLocales();
            shell.setCursor(null);
            busy.dispose();
          }
        });

    // ///////////////////////////////
    // START OF AddFileResult GROUP //
    // ///////////////////////////////

    Group wAddFileResult = new Group(wContentComp, SWT.SHADOW_NONE);
    PropsUi.setLook(wAddFileResult);
    wAddFileResult.setText(BaseMessages.getString(PKG, "TextFileInputDialog.wAddFileResult.Label"));

    FormLayout addFileResultgroupLayout = new FormLayout();
    addFileResultgroupLayout.marginWidth = 10;
    addFileResultgroupLayout.marginHeight = 10;
    wAddFileResult.setLayout(addFileResultgroupLayout);

    Label wlAddResult = new Label(wAddFileResult, SWT.RIGHT);
    wlAddResult.setText(BaseMessages.getString(PKG, "TextFileInputDialog.AddResult.Label"));
    PropsUi.setLook(wlAddResult);
    FormData fdlAddResult = new FormData();
    fdlAddResult.left = new FormAttachment(0, 0);
    fdlAddResult.top = new FormAttachment(wDateLocale, margin);
    fdlAddResult.right = new FormAttachment(middle, -margin);
    wlAddResult.setLayoutData(fdlAddResult);
    wAddResult = new Button(wAddFileResult, SWT.CHECK);
    PropsUi.setLook(wAddResult);
    wAddResult.setToolTipText(BaseMessages.getString(PKG, "TextFileInputDialog.AddResult.Tooltip"));
    FormData fdAddResult = new FormData();
    fdAddResult.left = new FormAttachment(middle, 0);
    fdAddResult.top = new FormAttachment(wlAddResult, 0, SWT.CENTER);
    wAddResult.setLayoutData(fdAddResult);

    FormData fdAddFileResult = new FormData();
    fdAddFileResult.left = new FormAttachment(0, margin);
    fdAddFileResult.top = new FormAttachment(wDateLocale, margin);
    fdAddFileResult.right = new FormAttachment(100, -margin);
    wAddFileResult.setLayoutData(fdAddFileResult);

    // ///////////////////////////////////////////////////////////
    // / END OF AddFileResult GROUP
    // ///////////////////////////////////////////////////////////

    wContentComp.pack();
    // What's the size:
    Rectangle bounds = wContentComp.getBounds();

    wContentSComp.setContent(wContentComp);
    wContentSComp.setExpandHorizontal(true);
    wContentSComp.setExpandVertical(true);
    wContentSComp.setMinWidth(bounds.width);
    wContentSComp.setMinHeight(bounds.height);

    FormData fdContentComp = new FormData();
    fdContentComp.left = new FormAttachment(0, 0);
    fdContentComp.top = new FormAttachment(0, 0);
    fdContentComp.right = new FormAttachment(100, 0);
    fdContentComp.bottom = new FormAttachment(100, 0);
    wContentComp.setLayoutData(fdContentComp);

    wContentTab.setControl(wContentSComp);

    // ///////////////////////////////////////////////////////////
    // / END OF CONTENT TAB
    // ///////////////////////////////////////////////////////////

  }

  protected void setLocales() {
    Locale[] locale = Locale.getAvailableLocales();
    String[] dateLocale = new String[locale.length];
    for (int i = 0; i < locale.length; i++) {
      dateLocale[i] = locale[i].toString();
    }
    if (dateLocale != null) {
      wDateLocale.setItems(dateLocale);
    }
  }

  private void addErrorTab() {
    // ////////////////////////
    // START OF ERROR TAB ///
    // /
    CTabItem wErrorTab = new CTabItem(wTabFolder, SWT.NONE);
    wErrorTab.setFont(GuiResource.getInstance().getFontDefault());
    wErrorTab.setText(BaseMessages.getString(PKG, "TextFileInputDialog.ErrorTab.TabTitle"));

    ScrolledComposite wErrorSComp = new ScrolledComposite(wTabFolder, SWT.V_SCROLL | SWT.H_SCROLL);
    wErrorSComp.setLayout(new FillLayout());

    FormLayout errorLayout = new FormLayout();
    errorLayout.marginWidth = 3;
    errorLayout.marginHeight = 3;

    Composite wErrorComp = new Composite(wErrorSComp, SWT.NONE);
    PropsUi.setLook(wErrorComp);
    wErrorComp.setLayout(errorLayout);

    // ERROR HANDLING...
    // ErrorIgnored?
    // ERROR HANDLING...
    Label wlErrorIgnored = new Label(wErrorComp, SWT.RIGHT);
    wlErrorIgnored.setText(BaseMessages.getString(PKG, "TextFileInputDialog.ErrorIgnored.Label"));
    PropsUi.setLook(wlErrorIgnored);
    FormData fdlErrorIgnored = new FormData();
    fdlErrorIgnored.left = new FormAttachment(0, 0);
    fdlErrorIgnored.top = new FormAttachment(0, margin);
    fdlErrorIgnored.right = new FormAttachment(middle, -margin);
    wlErrorIgnored.setLayoutData(fdlErrorIgnored);
    wErrorIgnored = new Button(wErrorComp, SWT.CHECK);
    PropsUi.setLook(wErrorIgnored);
    wErrorIgnored.setToolTipText(
        BaseMessages.getString(PKG, "TextFileInputDialog.ErrorIgnored.Tooltip"));
    FormData fdErrorIgnored = new FormData();
    fdErrorIgnored.left = new FormAttachment(middle, 0);
    fdErrorIgnored.top = new FormAttachment(wlErrorIgnored, 0, SWT.CENTER);
    wErrorIgnored.setLayoutData(fdErrorIgnored);

    // Skip bad files?
    Label wlSkipBadFiles = new Label(wErrorComp, SWT.RIGHT);
    wlSkipBadFiles.setText(BaseMessages.getString(PKG, "TextFileInputDialog.SkipBadFiles.Label"));
    PropsUi.setLook(wlSkipBadFiles);
    FormData fdlSkipBadFiles = new FormData();
    fdlSkipBadFiles.left = new FormAttachment(0, 0);
    fdlSkipBadFiles.top = new FormAttachment(wErrorIgnored, margin);
    fdlSkipBadFiles.right = new FormAttachment(middle, -margin);
    wlSkipBadFiles.setLayoutData(fdlSkipBadFiles);
    wSkipBadFiles = new Button(wErrorComp, SWT.CHECK);
    PropsUi.setLook(wSkipBadFiles);
    wSkipBadFiles.setToolTipText(
        BaseMessages.getString(PKG, "TextFileInputDialog.SkipBadFiles.Tooltip"));
    FormData fdSkipBadFiles = new FormData();
    fdSkipBadFiles.left = new FormAttachment(middle, 0);
    fdSkipBadFiles.top = new FormAttachment(wlSkipBadFiles, 0, SWT.CENTER);
    wSkipBadFiles.setLayoutData(fdSkipBadFiles);

    // field for rejected file
    Label wlBadFileField = new Label(wErrorComp, SWT.RIGHT);
    wlBadFileField.setText(BaseMessages.getString(PKG, "TextFileInputDialog.BadFileField.Label"));
    PropsUi.setLook(wlBadFileField);
    FormData fdlBadFileField = new FormData();
    fdlBadFileField.left = new FormAttachment(0, 0);
    fdlBadFileField.top = new FormAttachment(wSkipBadFiles, margin);
    fdlBadFileField.right = new FormAttachment(middle, -margin);
    wlBadFileField.setLayoutData(fdlBadFileField);
    wBadFileField = new Text(wErrorComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wBadFileField);
    wBadFileField.addModifyListener(lsMod);
    FormData fdBadFileField = new FormData();
    fdBadFileField.left = new FormAttachment(middle, 0);
    fdBadFileField.top = new FormAttachment(wSkipBadFiles, margin);
    fdBadFileField.right = new FormAttachment(100, 0);
    wBadFileField.setLayoutData(fdBadFileField);

    // field for file error messsage
    Label wlBadFileMessageField = new Label(wErrorComp, SWT.RIGHT);
    wlBadFileMessageField.setText(
        BaseMessages.getString(PKG, "TextFileInputDialog.BadFileMessageField.Label"));
    PropsUi.setLook(wlBadFileMessageField);
    FormData fdlBadFileMessageField = new FormData();
    fdlBadFileMessageField.left = new FormAttachment(0, 0);
    fdlBadFileMessageField.top = new FormAttachment(wBadFileField, margin);
    fdlBadFileMessageField.right = new FormAttachment(middle, -margin);
    wlBadFileMessageField.setLayoutData(fdlBadFileMessageField);
    wBadFileMessageField = new Text(wErrorComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wBadFileMessageField);
    wBadFileMessageField.addModifyListener(lsMod);
    FormData fdBadFileMessageField = new FormData();
    fdBadFileMessageField.left = new FormAttachment(middle, 0);
    fdBadFileMessageField.top = new FormAttachment(wBadFileField, margin);
    fdBadFileMessageField.right = new FormAttachment(100, 0);
    wBadFileMessageField.setLayoutData(fdBadFileMessageField);

    // Skip error lines?
    wlSkipErrorLines = new Label(wErrorComp, SWT.RIGHT);
    wlSkipErrorLines.setText(
        BaseMessages.getString(PKG, "TextFileInputDialog.SkipErrorLines.Label"));
    PropsUi.setLook(wlSkipErrorLines);
    FormData fdlSkipErrorLines = new FormData();
    fdlSkipErrorLines.left = new FormAttachment(0, 0);
    fdlSkipErrorLines.top = new FormAttachment(wBadFileMessageField, margin);
    fdlSkipErrorLines.right = new FormAttachment(middle, -margin);
    wlSkipErrorLines.setLayoutData(fdlSkipErrorLines);
    wSkipErrorLines = new Button(wErrorComp, SWT.CHECK);
    PropsUi.setLook(wSkipErrorLines);
    wSkipErrorLines.setToolTipText(
        BaseMessages.getString(PKG, "TextFileInputDialog.SkipErrorLines.Tooltip"));
    FormData fdSkipErrorLines = new FormData();
    fdSkipErrorLines.left = new FormAttachment(middle, 0);
    fdSkipErrorLines.top = new FormAttachment(wlSkipErrorLines, 0, SWT.CENTER);
    wSkipErrorLines.setLayoutData(fdSkipErrorLines);

    wlErrorCount = new Label(wErrorComp, SWT.RIGHT);
    wlErrorCount.setText(BaseMessages.getString(PKG, "TextFileInputDialog.ErrorCount.Label"));
    PropsUi.setLook(wlErrorCount);
    FormData fdlErrorCount = new FormData();
    fdlErrorCount.left = new FormAttachment(0, 0);
    fdlErrorCount.top = new FormAttachment(wSkipErrorLines, margin);
    fdlErrorCount.right = new FormAttachment(middle, -margin);
    wlErrorCount.setLayoutData(fdlErrorCount);
    wErrorCount = new Text(wErrorComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wErrorCount);
    wErrorCount.addModifyListener(lsMod);
    FormData fdErrorCount = new FormData();
    fdErrorCount.left = new FormAttachment(middle, 0);
    fdErrorCount.top = new FormAttachment(wSkipErrorLines, margin);
    fdErrorCount.right = new FormAttachment(100, 0);
    wErrorCount.setLayoutData(fdErrorCount);

    wlErrorFields = new Label(wErrorComp, SWT.RIGHT);
    wlErrorFields.setText(BaseMessages.getString(PKG, "TextFileInputDialog.ErrorFields.Label"));
    PropsUi.setLook(wlErrorFields);
    FormData fdlErrorFields = new FormData();
    fdlErrorFields.left = new FormAttachment(0, 0);
    fdlErrorFields.top = new FormAttachment(wErrorCount, margin);
    fdlErrorFields.right = new FormAttachment(middle, -margin);
    wlErrorFields.setLayoutData(fdlErrorFields);
    wErrorFields = new Text(wErrorComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wErrorFields);
    wErrorFields.addModifyListener(lsMod);
    FormData fdErrorFields = new FormData();
    fdErrorFields.left = new FormAttachment(middle, 0);
    fdErrorFields.top = new FormAttachment(wErrorCount, margin);
    fdErrorFields.right = new FormAttachment(100, 0);
    wErrorFields.setLayoutData(fdErrorFields);

    wlErrorText = new Label(wErrorComp, SWT.RIGHT);
    wlErrorText.setText(BaseMessages.getString(PKG, "TextFileInputDialog.ErrorText.Label"));
    PropsUi.setLook(wlErrorText);
    FormData fdlErrorText = new FormData();
    fdlErrorText.left = new FormAttachment(0, 0);
    fdlErrorText.top = new FormAttachment(wErrorFields, margin);
    fdlErrorText.right = new FormAttachment(middle, -margin);
    wlErrorText.setLayoutData(fdlErrorText);
    wErrorText = new Text(wErrorComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wErrorText);
    wErrorText.addModifyListener(lsMod);
    FormData fdErrorText = new FormData();
    fdErrorText.left = new FormAttachment(middle, 0);
    fdErrorText.top = new FormAttachment(wErrorFields, margin);
    fdErrorText.right = new FormAttachment(100, 0);
    wErrorText.setLayoutData(fdErrorText);

    // Bad lines files directory + extension
    Control previous = wErrorText;

    // BadDestDir line
    wlWarnDestDir = new Label(wErrorComp, SWT.RIGHT);
    wlWarnDestDir.setText(BaseMessages.getString(PKG, "TextFileInputDialog.WarnDestDir.Label"));
    PropsUi.setLook(wlWarnDestDir);
    FormData fdlWarnDestDir = new FormData();
    fdlWarnDestDir.left = new FormAttachment(0, 0);
    fdlWarnDestDir.top = new FormAttachment(previous, margin * 4);
    fdlWarnDestDir.right = new FormAttachment(middle, -margin);
    wlWarnDestDir.setLayoutData(fdlWarnDestDir);

    wbbWarnDestDir = new Button(wErrorComp, SWT.PUSH | SWT.CENTER);
    PropsUi.setLook(wbbWarnDestDir);
    wbbWarnDestDir.setText(BaseMessages.getString(PKG, CONST_SYSTEM_BUTTON_BROWSE));
    wbbWarnDestDir.setToolTipText(BaseMessages.getString(PKG, "System.Tooltip.BrowseForDir"));
    FormData fdbBadDestDir = new FormData();
    fdbBadDestDir.right = new FormAttachment(100, 0);
    fdbBadDestDir.top = new FormAttachment(previous, margin * 4);
    wbbWarnDestDir.setLayoutData(fdbBadDestDir);

    wWarnExt = new Text(wErrorComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wWarnExt);
    wWarnExt.addModifyListener(lsMod);
    FormData fdWarnDestExt = new FormData();
    fdWarnDestExt.left = new FormAttachment(wbbWarnDestDir, -150);
    fdWarnDestExt.right = new FormAttachment(wbbWarnDestDir, -margin);
    fdWarnDestExt.top = new FormAttachment(previous, margin * 4);
    wWarnExt.setLayoutData(fdWarnDestExt);

    wlWarnExt = new Label(wErrorComp, SWT.RIGHT);
    wlWarnExt.setText(BaseMessages.getString(PKG, CONST_SYSTEM_LABEL_EXTENSION));
    PropsUi.setLook(wlWarnExt);
    FormData fdlWarnDestExt = new FormData();
    fdlWarnDestExt.top = new FormAttachment(previous, margin * 4);
    fdlWarnDestExt.right = new FormAttachment(wWarnExt, -margin);
    wlWarnExt.setLayoutData(fdlWarnDestExt);

    wWarnDestDir = new TextVar(variables, wErrorComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wWarnDestDir);
    wWarnDestDir.addModifyListener(lsMod);
    FormData fdBadDestDir = new FormData();
    fdBadDestDir.left = new FormAttachment(middle, 0);
    fdBadDestDir.right = new FormAttachment(wlWarnExt, -margin);
    fdBadDestDir.top = new FormAttachment(previous, margin * 4);
    wWarnDestDir.setLayoutData(fdBadDestDir);

    // Listen to the Browse... button
    wbbWarnDestDir.addSelectionListener(
        DirectoryDialogButtonListenerFactory.getSelectionAdapter(shell, wWarnDestDir));

    // Whenever something changes, set the tooltip to the expanded version of the directory:
    wWarnDestDir.addModifyListener(getModifyListenerTooltipText(variables, wWarnDestDir));

    // Error lines files directory + extension
    previous = wWarnDestDir;

    // ErrorDestDir line
    wlErrorDestDir = new Label(wErrorComp, SWT.RIGHT);
    wlErrorDestDir.setText(BaseMessages.getString(PKG, "TextFileInputDialog.ErrorDestDir.Label"));
    PropsUi.setLook(wlErrorDestDir);
    FormData fdlErrorDestDir = new FormData();
    fdlErrorDestDir.left = new FormAttachment(0, 0);
    fdlErrorDestDir.top = new FormAttachment(previous, margin);
    fdlErrorDestDir.right = new FormAttachment(middle, -margin);
    wlErrorDestDir.setLayoutData(fdlErrorDestDir);

    wbbErrorDestDir = new Button(wErrorComp, SWT.PUSH | SWT.CENTER);
    PropsUi.setLook(wbbErrorDestDir);
    wbbErrorDestDir.setText(BaseMessages.getString(PKG, CONST_SYSTEM_BUTTON_BROWSE));
    wbbErrorDestDir.setToolTipText(BaseMessages.getString(PKG, "System.Tooltip.BrowseForDir"));
    FormData fdbErrorDestDir = new FormData();
    fdbErrorDestDir.right = new FormAttachment(100, 0);
    fdbErrorDestDir.top = new FormAttachment(previous, margin);
    wbbErrorDestDir.setLayoutData(fdbErrorDestDir);

    wErrorExt = new Text(wErrorComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wErrorExt);
    wErrorExt.addModifyListener(lsMod);
    FormData fdErrorDestExt = new FormData();
    fdErrorDestExt.left = new FormAttachment(wWarnExt, 0, SWT.LEFT);
    fdErrorDestExt.right = new FormAttachment(wWarnExt, 0, SWT.RIGHT);
    fdErrorDestExt.top = new FormAttachment(previous, margin);
    wErrorExt.setLayoutData(fdErrorDestExt);

    wlErrorExt = new Label(wErrorComp, SWT.RIGHT);
    wlErrorExt.setText(BaseMessages.getString(PKG, CONST_SYSTEM_LABEL_EXTENSION));
    PropsUi.setLook(wlErrorExt);
    FormData fdlErrorDestExt = new FormData();
    fdlErrorDestExt.top = new FormAttachment(previous, margin);
    fdlErrorDestExt.right = new FormAttachment(wErrorExt, -margin);
    wlErrorExt.setLayoutData(fdlErrorDestExt);

    wErrorDestDir = new TextVar(variables, wErrorComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wErrorDestDir);
    wErrorDestDir.addModifyListener(lsMod);
    FormData fdErrorDestDir = new FormData();
    fdErrorDestDir.left = new FormAttachment(middle, 0);
    fdErrorDestDir.right = new FormAttachment(wlErrorExt, -margin);
    fdErrorDestDir.top = new FormAttachment(previous, margin);
    wErrorDestDir.setLayoutData(fdErrorDestDir);

    // Listen to the Browse... button
    wbbErrorDestDir.addSelectionListener(
        DirectoryDialogButtonListenerFactory.getSelectionAdapter(shell, wErrorDestDir));

    // Whenever something changes, set the tooltip to the expanded version of the directory:
    wErrorDestDir.addModifyListener(getModifyListenerTooltipText(variables, wErrorDestDir));

    // Data Error lines files directory + extension
    previous = wErrorDestDir;

    // LineNrDestDir line
    wlLineNrDestDir = new Label(wErrorComp, SWT.RIGHT);
    wlLineNrDestDir.setText(BaseMessages.getString(PKG, "TextFileInputDialog.LineNrDestDir.Label"));
    PropsUi.setLook(wlLineNrDestDir);
    FormData fdlLineNrDestDir = new FormData();
    fdlLineNrDestDir.left = new FormAttachment(0, 0);
    fdlLineNrDestDir.top = new FormAttachment(previous, margin);
    fdlLineNrDestDir.right = new FormAttachment(middle, -margin);
    wlLineNrDestDir.setLayoutData(fdlLineNrDestDir);

    wbbLineNrDestDir = new Button(wErrorComp, SWT.PUSH | SWT.CENTER);
    PropsUi.setLook(wbbLineNrDestDir);
    wbbLineNrDestDir.setText(BaseMessages.getString(PKG, CONST_SYSTEM_BUTTON_BROWSE));
    wbbLineNrDestDir.setToolTipText(BaseMessages.getString(PKG, "System.Tooltip.Browse"));
    FormData fdbLineNrDestDir = new FormData();
    fdbLineNrDestDir.right = new FormAttachment(100, 0);
    fdbLineNrDestDir.top = new FormAttachment(previous, margin);
    wbbLineNrDestDir.setLayoutData(fdbLineNrDestDir);

    wLineNrExt = new Text(wErrorComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wLineNrExt);
    wLineNrExt.addModifyListener(lsMod);
    FormData fdLineNrDestExt = new FormData();
    fdLineNrDestExt.left = new FormAttachment(wErrorExt, 0, SWT.LEFT);
    fdLineNrDestExt.right = new FormAttachment(wErrorExt, 0, SWT.RIGHT);
    fdLineNrDestExt.top = new FormAttachment(previous, margin);
    wLineNrExt.setLayoutData(fdLineNrDestExt);

    wlLineNrExt = new Label(wErrorComp, SWT.RIGHT);
    wlLineNrExt.setText(BaseMessages.getString(PKG, CONST_SYSTEM_LABEL_EXTENSION));
    PropsUi.setLook(wlLineNrExt);
    FormData fdlLineNrDestExt = new FormData();
    fdlLineNrDestExt.top = new FormAttachment(previous, margin);
    fdlLineNrDestExt.right = new FormAttachment(wLineNrExt, -margin);
    wlLineNrExt.setLayoutData(fdlLineNrDestExt);

    wLineNrDestDir = new TextVar(variables, wErrorComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wLineNrDestDir);
    wLineNrDestDir.addModifyListener(lsMod);
    FormData fdLineNrDestDir = new FormData();
    fdLineNrDestDir.left = new FormAttachment(middle, 0);
    fdLineNrDestDir.right = new FormAttachment(wlLineNrExt, -margin);
    fdLineNrDestDir.top = new FormAttachment(previous, margin);
    wLineNrDestDir.setLayoutData(fdLineNrDestDir);

    // Listen to the Browse... button
    wbbLineNrDestDir.addSelectionListener(
        DirectoryDialogButtonListenerFactory.getSelectionAdapter(shell, wLineNrDestDir));

    // Whenever something changes, set the tooltip to the expanded version of the directory:
    wLineNrDestDir.addModifyListener(getModifyListenerTooltipText(variables, wLineNrDestDir));

    FormData fdErrorComp = new FormData();
    fdErrorComp.left = new FormAttachment(0, 0);
    fdErrorComp.top = new FormAttachment(0, 0);
    fdErrorComp.right = new FormAttachment(100, 0);
    fdErrorComp.bottom = new FormAttachment(100, 0);
    wErrorComp.setLayoutData(fdErrorComp);

    wErrorComp.pack();
    // What's the size:
    Rectangle bounds = wErrorComp.getBounds();

    wErrorSComp.setContent(wErrorComp);
    wErrorSComp.setExpandHorizontal(true);
    wErrorSComp.setExpandVertical(true);
    wErrorSComp.setMinWidth(bounds.width);
    wErrorSComp.setMinHeight(bounds.height);

    wErrorTab.setControl(wErrorSComp);

    // ///////////////////////////////////////////////////////////
    // / END OF CONTENT TAB
    // ///////////////////////////////////////////////////////////

  }

  private void addFiltersTabs() {
    // Filters tab...
    //
    CTabItem wFilterTab = new CTabItem(wTabFolder, SWT.NONE);
    wFilterTab.setFont(GuiResource.getInstance().getFontDefault());
    wFilterTab.setText(BaseMessages.getString(PKG, "TextFileInputDialog.FilterTab.TabTitle"));

    FormLayout filterLayout = new FormLayout();
    filterLayout.marginWidth = PropsUi.getFormMargin();
    filterLayout.marginHeight = PropsUi.getFormMargin();

    Composite wFilterComp = new Composite(wTabFolder, SWT.NONE);
    wFilterComp.setLayout(filterLayout);
    PropsUi.setLook(wFilterComp);

    final int FilterRows = input.getFilter().length;

    ColumnInfo[] colinf =
        new ColumnInfo[] {
          new ColumnInfo(
              BaseMessages.getString(PKG, "TextFileInputDialog.FilterStringColumn.Column"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "TextFileInputDialog.FilterPositionColumn.Column"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "TextFileInputDialog.StopOnFilterColumn.Column"),
              ColumnInfo.COLUMN_TYPE_CCOMBO,
              YES_NO_COMBO),
          new ColumnInfo(
              BaseMessages.getString(PKG, "TextFileInputDialog.FilterPositiveColumn.Column"),
              ColumnInfo.COLUMN_TYPE_CCOMBO,
              YES_NO_COMBO)
        };

    colinf[2].setToolTip(
        BaseMessages.getString(PKG, "TextFileInputDialog.StopOnFilterColumn.Tooltip"));
    colinf[3].setToolTip(
        BaseMessages.getString(PKG, "TextFileInputDialog.FilterPositiveColumn.Tooltip"));

    wFilter =
        new TableView(
            variables,
            wFilterComp,
            SWT.FULL_SELECTION | SWT.MULTI,
            colinf,
            FilterRows,
            lsMod,
            props);

    FormData fdFilter = new FormData();
    fdFilter.left = new FormAttachment(0, 0);
    fdFilter.top = new FormAttachment(0, 0);
    fdFilter.right = new FormAttachment(100, 0);
    fdFilter.bottom = new FormAttachment(100, 0);
    wFilter.setLayoutData(fdFilter);

    FormData fdFilterComp = new FormData();
    fdFilterComp.left = new FormAttachment(0, 0);
    fdFilterComp.top = new FormAttachment(0, 0);
    fdFilterComp.right = new FormAttachment(100, 0);
    fdFilterComp.bottom = new FormAttachment(100, 0);
    wFilterComp.setLayoutData(fdFilterComp);

    wFilterComp.layout();
    wFilterTab.setControl(wFilterComp);
  }

  private void addFieldsTabs() {

    SelectionListener lsSelection =
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            fillFieldsLayoutFromSchema();
            input.setChanged();
          }
        };

    // Fields tab...
    //
    CTabItem wFieldsTab = new CTabItem(wTabFolder, SWT.NONE);
    wFieldsTab.setFont(GuiResource.getInstance().getFontDefault());
    wFieldsTab.setText(BaseMessages.getString(PKG, "TextFileInputDialog.FieldsTab.TabTitle"));

    FormLayout fieldsLayout = new FormLayout();
    fieldsLayout.marginWidth = PropsUi.getFormMargin();
    fieldsLayout.marginHeight = PropsUi.getFormMargin();

    Composite wFieldsComp = new Composite(wTabFolder, SWT.NONE);
    wFieldsComp.setLayout(fieldsLayout);
    PropsUi.setLook(wFieldsComp);

    wSchemaDefinition =
        new MetaSelectionLine<>(
            variables,
            metadataProvider,
            SchemaDefinition.class,
            wFieldsComp,
            SWT.NONE,
            BaseMessages.getString(PKG, "TextFileInputDialog.SchemaDefinition.Label"),
            BaseMessages.getString(PKG, "TextFileInputDialog.SchemaDefinition.Tooltip"));

    PropsUi.setLook(wSchemaDefinition);
    FormData fdSchemaDefinition = new FormData();
    fdSchemaDefinition.left = new FormAttachment(0, 0);
    fdSchemaDefinition.top = new FormAttachment(0, margin);
    fdSchemaDefinition.right = new FormAttachment(100, 0);
    wSchemaDefinition.setLayoutData(fdSchemaDefinition);

    try {
      wSchemaDefinition.fillItems();
    } catch (Exception e) {
      log.logError("Error getting schema definition items", e);
    }

    wSchemaDefinition.addSelectionListener(lsSelection);

    Group wManualSchemaDefinition = new Group(wFieldsComp, SWT.SHADOW_NONE);
    PropsUi.setLook(wManualSchemaDefinition);
    wManualSchemaDefinition.setText(
        BaseMessages.getString(PKG, "TextFileInputDialog.ManualSchemaDefinition.Label"));

    FormLayout manualSchemaDefinitionLayout = new FormLayout();
    manualSchemaDefinitionLayout.marginWidth = 10;
    manualSchemaDefinitionLayout.marginHeight = 10;
    wManualSchemaDefinition.setLayout(manualSchemaDefinitionLayout);

    wGet = new Button(wManualSchemaDefinition, SWT.PUSH);
    wGet.setText(BaseMessages.getString(PKG, "System.Button.GetFields"));
    fdGet = new FormData();
    fdGet.left = new FormAttachment(50, 0);
    fdGet.bottom = new FormAttachment(100, 0);
    wGet.setLayoutData(fdGet);

    wMinWidth = new Button(wManualSchemaDefinition, SWT.PUSH);
    wMinWidth.setText(BaseMessages.getString(PKG, "TextFileInputDialog.MinWidth.Button"));
    wMinWidth.setToolTipText(BaseMessages.getString(PKG, "TextFileInputDialog.MinWidth.Tooltip"));
    wMinWidth.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            input.setChanged();
          }
        });
    setButtonPositions(new Button[] {wGet, wMinWidth}, margin, null);

    final int FieldsRows = input.inputFields.length;

    ColumnInfo[] colinf =
        new ColumnInfo[] {
          new ColumnInfo(
              BaseMessages.getString(PKG, "TextFileInputDialog.NameColumn.Column"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "TextFileInputDialog.TypeColumn.Column"),
              ColumnInfo.COLUMN_TYPE_CCOMBO,
              ValueMetaFactory.getValueMetaNames(),
              true),
          new ColumnInfo(
              BaseMessages.getString(PKG, "TextFileInputDialog.FormatColumn.Column"),
              ColumnInfo.COLUMN_TYPE_FORMAT,
              2),
          new ColumnInfo(
              BaseMessages.getString(PKG, "TextFileInputDialog.PositionColumn.Column"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "TextFileInputDialog.LengthColumn.Column"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "TextFileInputDialog.PrecisionColumn.Column"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "TextFileInputDialog.CurrencyColumn.Column"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "TextFileInputDialog.DecimalColumn.Column"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "TextFileInputDialog.GroupColumn.Column"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "TextFileInputDialog.NullIfColumn.Column"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "TextFileInputDialog.IfNullColumn.Column"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "TextFileInputDialog.TrimTypeColumn.Column"),
              ColumnInfo.COLUMN_TYPE_CCOMBO,
              ValueMetaBase.trimTypeDesc,
              true),
          new ColumnInfo(
              BaseMessages.getString(PKG, "TextFileInputDialog.RepeatColumn.Column"),
              ColumnInfo.COLUMN_TYPE_CCOMBO,
              new String[] {
                BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_YES),
                BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_NO)
              },
              true)
        };

    colinf[12].setToolTip(BaseMessages.getString(PKG, "TextFileInputDialog.RepeatColumn.Tooltip"));

    wFields =
        new TableView(
            variables,
            wManualSchemaDefinition,
            SWT.FULL_SELECTION | SWT.MULTI,
            colinf,
            FieldsRows,
            lsMod,
            props);

    FormData fdFields = new FormData();
    fdFields.left = new FormAttachment(0, 0);
    fdFields.top = new FormAttachment(0, 0);
    fdFields.right = new FormAttachment(100, 0);
    fdFields.bottom = new FormAttachment(wGet, -margin);
    wFields.setLayoutData(fdFields);

    FormData fdManualSchemaDefinitionComp = new FormData();
    fdManualSchemaDefinitionComp.left = new FormAttachment(0, 0);
    fdManualSchemaDefinitionComp.top = new FormAttachment(wSchemaDefinition, 0);
    fdManualSchemaDefinitionComp.right = new FormAttachment(100, 0);
    fdManualSchemaDefinitionComp.bottom = new FormAttachment(100, 0);
    wManualSchemaDefinition.setLayoutData(fdManualSchemaDefinitionComp);

    FormData fdFieldsComp = new FormData();
    fdFieldsComp.left = new FormAttachment(0, 0);
    fdFieldsComp.top = new FormAttachment(0, 0);
    fdFieldsComp.right = new FormAttachment(100, 0);
    fdFieldsComp.bottom = new FormAttachment(100, 0);
    wFieldsComp.setLayoutData(fdFieldsComp);

    wFieldsComp.layout();
    wFieldsTab.setControl(wFieldsComp);
  }

  private void fillFieldsLayoutFromSchema() {

    if (!wSchemaDefinition.isDisposed()) {
      final String schemaName = wSchemaDefinition.getText();

      MessageBox mb = new MessageBox(shell, SWT.ICON_QUESTION | SWT.NO | SWT.YES);
      mb.setMessage(
          BaseMessages.getString(
              PKG, "TextFileInputDialog.Load.SchemaDefinition.Message", schemaName));
      mb.setText(BaseMessages.getString(PKG, "TextFileInputDialog.Load.SchemaDefinition.Title"));
      int answer = mb.open();

      if (answer == SWT.YES && !Utils.isEmpty(schemaName)) {
        try {
          SchemaDefinition schemaDefinition =
              (new SchemaDefinitionUtil()).loadSchemaDefinition(metadataProvider, schemaName);
          if (schemaDefinition != null) {
            IRowMeta r = schemaDefinition.getRowMeta();
            if (r != null) {
              String[] fieldNames = r.getFieldNames();
              if (fieldNames != null) {
                wFields.clearAll();
                for (int i = 0; i < fieldNames.length; i++) {
                  IValueMeta valueMeta = r.getValueMeta(i);
                  final TableItem item = getTableItem(valueMeta.getName(), true);
                  item.setText(1, valueMeta.getName());
                  item.setText(2, ValueMetaFactory.getValueMetaName(valueMeta.getType()));
                  item.setText(3, Const.NVL(valueMeta.getConversionMask(), ""));
                  item.setText(
                      5, valueMeta.getLength() >= 0 ? Integer.toString(valueMeta.getLength()) : "");
                  item.setText(
                      6,
                      valueMeta.getPrecision() >= 0
                          ? Integer.toString(valueMeta.getPrecision())
                          : "");
                  final SchemaFieldDefinition schemaFieldDefinition =
                      schemaDefinition.getFieldDefinitions().get(i);
                  item.setText(7, Const.NVL(schemaFieldDefinition.getCurrencySymbol(), ""));
                  item.setText(8, Const.NVL(schemaFieldDefinition.getDecimalSymbol(), ""));
                  item.setText(9, Const.NVL(schemaFieldDefinition.getGroupingSymbol(), ""));
                  item.setText(10, Const.NVL(schemaFieldDefinition.getIfNullValue(), ""));
                  item.setText(
                      12, Const.NVL(ValueMetaBase.getTrimTypeDesc(valueMeta.getTrimType()), ""));
                }
              }
            }
          }
        } catch (HopTransformException | HopPluginException e) {

          // ignore any errors here.
        }

        wFields.removeEmptyRows();
        wFields.setRowNums();
        wFields.optWidth(true);
      }
    }
  }

  public void setFlags() {
    boolean accept = wAccFilenames.getSelection();
    wlPassThruFields.setEnabled(accept);
    wPassThruFields.setEnabled(accept);
    if (!wAccFilenames.getSelection()) {
      wPassThruFields.setSelection(false);
    }
    wlAccField.setEnabled(accept);
    wAccField.setEnabled(accept);
    wlAccTransform.setEnabled(accept);
    wAccTransform.setEnabled(accept);

    wlFilename.setEnabled(!accept);
    wbbFilename.setEnabled(!accept); // Browse: add file or directory
    wbdFilename.setEnabled(!accept); // Delete
    wbeFilename.setEnabled(!accept); // Edit
    wbaFilename.setEnabled(!accept); // Add or change
    wFilename.setEnabled(!accept);
    wlFilenameList.setEnabled(!accept);
    wFilenameList.setEnabled(!accept);
    wlFilemask.setEnabled(!accept);
    wFilemask.setEnabled(!accept);
    wbShowFiles.setEnabled(!accept);

    wFirst.setEnabled(!accept);
    wFirstHeader.setEnabled(!accept);

    wlInclFilenameField.setEnabled(wInclFilename.getSelection());
    wInclFilenameField.setEnabled(wInclFilename.getSelection());

    wlInclRownumField.setEnabled(wInclRownum.getSelection());
    wInclRownumField.setEnabled(wInclRownum.getSelection());
    wlRownumByFileField.setEnabled(wInclRownum.getSelection());
    wRownumByFile.setEnabled(wInclRownum.getSelection());

    // Error handling tab...
    wlSkipErrorLines.setEnabled(wErrorIgnored.getSelection());
    wSkipBadFiles.setEnabled(wErrorIgnored.getSelection());
    wBadFileField.setEnabled(wErrorIgnored.getSelection() && wSkipBadFiles.getSelection());
    wBadFileMessageField.setEnabled(wErrorIgnored.getSelection() && wSkipBadFiles.getSelection());
    wSkipErrorLines.setEnabled(wErrorIgnored.getSelection());
    wlErrorCount.setEnabled(wErrorIgnored.getSelection());
    wErrorCount.setEnabled(wErrorIgnored.getSelection());
    wlErrorFields.setEnabled(wErrorIgnored.getSelection());
    wErrorFields.setEnabled(wErrorIgnored.getSelection());
    wlErrorText.setEnabled(wErrorIgnored.getSelection());
    wErrorText.setEnabled(wErrorIgnored.getSelection());

    wlWarnDestDir.setEnabled(wErrorIgnored.getSelection());
    wWarnDestDir.setEnabled(wErrorIgnored.getSelection());
    wlWarnExt.setEnabled(wErrorIgnored.getSelection());
    wWarnExt.setEnabled(wErrorIgnored.getSelection());
    wbbWarnDestDir.setEnabled(wErrorIgnored.getSelection());

    wlErrorDestDir.setEnabled(wErrorIgnored.getSelection());
    wErrorDestDir.setEnabled(wErrorIgnored.getSelection());
    wlErrorExt.setEnabled(wErrorIgnored.getSelection());
    wErrorExt.setEnabled(wErrorIgnored.getSelection());
    wbbErrorDestDir.setEnabled(wErrorIgnored.getSelection());

    wlLineNrDestDir.setEnabled(wErrorIgnored.getSelection());
    wLineNrDestDir.setEnabled(wErrorIgnored.getSelection());
    wlLineNrExt.setEnabled(wErrorIgnored.getSelection());
    wLineNrExt.setEnabled(wErrorIgnored.getSelection());
    wbbLineNrDestDir.setEnabled(wErrorIgnored.getSelection());

    wlNrHeader.setEnabled(wHeader.getSelection());
    wNrHeader.setEnabled(wHeader.getSelection());
    wlNrFooter.setEnabled(wFooter.getSelection());
    wNrFooter.setEnabled(wFooter.getSelection());
    wlNrWraps.setEnabled(wWraps.getSelection());
    wNrWraps.setEnabled(wWraps.getSelection());

    wlNrLinesPerPage.setEnabled(wLayoutPaged.getSelection());
    wNrLinesPerPage.setEnabled(wLayoutPaged.getSelection());
    wlNrLinesDocHeader.setEnabled(wLayoutPaged.getSelection());
    wNrLinesDocHeader.setEnabled(wLayoutPaged.getSelection());
  }

  /**
   * Read the data from the TextFileInputMeta object and show it in this dialog.
   *
   * @param meta The TextFileInputMeta object to obtain the data from.
   */
  public void getData(TextFileInputMeta meta) {
    getData(meta, true, true, null);
  }

  @Override
  public void getData(
      final TextFileInputMeta meta,
      final boolean copyTransformName,
      final boolean reloadAllFields,
      final List<String> newFieldNames) {
    if (copyTransformName) {
      wTransformName.setText(transformName);
    }

    wAccFilenames.setSelection(meta.inputFiles.acceptingFilenames);
    wPassThruFields.setSelection(meta.inputFiles.passingThruFields);
    if (meta.inputFiles.acceptingField != null) {
      wAccField.setText(meta.inputFiles.acceptingField);
    }
    if (meta.getAcceptingTransform() != null) {
      wAccTransform.setText(meta.getAcceptingTransform().getName());
    }

    wSchemaDefinition.setText(Const.NVL(meta.getSchemaDefinition(), ""));

    if (meta.getFileName() != null) {
      wFilenameList.removeAll();

      for (int i = 0; i < meta.getFileName().length; i++) {
        wFilenameList.add(
            meta.getFileName()[i],
            meta.inputFiles.fileMask[i],
            meta.inputFiles.excludeFileMask[i],
            meta.getRequiredFilesDesc(meta.inputFiles.fileRequired[i]),
            meta.getRequiredFilesDesc(meta.inputFiles.includeSubFolders[i]));
      }
      wFilenameList.removeEmptyRows();
      wFilenameList.setRowNums();
      wFilenameList.optWidth(true);
    }
    if (meta.content.fileType != null) {
      wFiletype.setText(meta.content.fileType);
    }
    if (meta.content.separator != null) {
      wSeparator.setText(meta.content.separator);
    }
    if (meta.content.enclosure != null) {
      wEnclosure.setText(meta.content.enclosure);
    }
    if (meta.content.escapeCharacter != null) {
      wEscape.setText(meta.content.escapeCharacter);
    }
    wEnclBreaks.setSelection(meta.content.breakInEnclosureAllowed);
    wPrependFileName.setSelection(meta.content.prependFileName);
    wHeader.setSelection(meta.content.header);
    wNrHeader.setText("" + meta.content.nrHeaderLines);
    wFooter.setSelection(meta.content.footer);
    wNrFooter.setText("" + meta.content.nrFooterLines);
    wWraps.setSelection(meta.content.lineWrapped);
    wNrWraps.setText("" + meta.content.nrWraps);
    wLayoutPaged.setSelection(meta.content.layoutPaged);
    wNrLinesPerPage.setText("" + meta.content.nrLinesPerPage);
    wNrLinesDocHeader.setText("" + meta.content.nrLinesDocHeader);
    if (meta.content.fileCompression != null) {
      wCompression.setText(meta.content.fileCompression);
    }
    wNoempty.setSelection(meta.content.noEmptyLines);
    wInclFilename.setSelection(meta.content.includeFilename);
    wInclRownum.setSelection(meta.content.includeRowNumber);
    wRownumByFile.setSelection(meta.content.rowNumberByFile);
    wDateLenient.setSelection(meta.content.dateFormatLenient);
    wAddResult.setSelection(meta.inputFiles.isaddresult);

    if (meta.content.filenameField != null) {
      wInclFilenameField.setText(meta.content.filenameField);
    }
    if (meta.content.rowNumberField != null) {
      wInclRownumField.setText(meta.content.rowNumberField);
    }
    if (meta.content.fileFormat != null) {
      wFormat.setText(meta.content.fileFormat);
    }

    if (meta.content.length != null) {
      wLength.setText(meta.content.length);
    }

    wLimit.setText("" + meta.content.rowLimit);

    logDebug("getting fields info...");
    getFieldsData(meta, false, reloadAllFields, newFieldNames);

    if (meta.getEncoding() != null) {
      wEncoding.setText(meta.getEncoding());
    }

    // Error handling fields...
    wErrorIgnored.setSelection(meta.errorHandling.errorIgnored);
    wSkipBadFiles.setSelection(meta.errorHandling.skipBadFiles);
    wSkipErrorLines.setSelection(meta.isErrorLineSkipped());

    if (meta.errorHandling.fileErrorField != null) {
      wBadFileField.setText(meta.errorHandling.fileErrorField);
    }
    if (meta.errorHandling.fileErrorMessageField != null) {
      wBadFileMessageField.setText(meta.errorHandling.fileErrorMessageField);
    }

    if (meta.getErrorCountField() != null) {
      wErrorCount.setText(meta.getErrorCountField());
    }
    if (meta.getErrorFieldsField() != null) {
      wErrorFields.setText(meta.getErrorFieldsField());
    }
    if (meta.getErrorTextField() != null) {
      wErrorText.setText(meta.getErrorTextField());
    }

    if (meta.errorHandling.warningFilesDestinationDirectory != null) {
      wWarnDestDir.setText(meta.errorHandling.warningFilesDestinationDirectory);
    }
    if (meta.errorHandling.warningFilesExtension != null) {
      wWarnExt.setText(meta.errorHandling.warningFilesExtension);
    }

    if (meta.errorHandling.errorFilesDestinationDirectory != null) {
      wErrorDestDir.setText(meta.errorHandling.errorFilesDestinationDirectory);
    }
    if (meta.errorHandling.errorFilesExtension != null) {
      wErrorExt.setText(meta.errorHandling.errorFilesExtension);
    }

    if (meta.errorHandling.lineNumberFilesDestinationDirectory != null) {
      wLineNrDestDir.setText(meta.errorHandling.lineNumberFilesDestinationDirectory);
    }
    if (meta.errorHandling.lineNumberFilesExtension != null) {
      wLineNrExt.setText(meta.errorHandling.lineNumberFilesExtension);
    }

    for (int i = 0; i < meta.getFilter().length; i++) {
      TableItem item = wFilter.table.getItem(i);

      TextFileFilter filter = meta.getFilter()[i];
      if (filter.getFilterString() != null) {
        item.setText(1, filter.getFilterString());
      }
      if (filter.getFilterPosition() >= 0) {
        item.setText(2, "" + filter.getFilterPosition());
      }
      item.setText(
          3,
          filter.isFilterLastLine()
              ? BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_YES)
              : BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_NO));
      item.setText(
          4,
          filter.isFilterPositive()
              ? BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_YES)
              : BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_NO));
    }

    // Date locale
    wDateLocale.setText(meta.content.dateFormatLocale.toString());

    wFields.removeEmptyRows();
    wFields.setRowNums();
    wFields.optWidth(true);

    wFilter.removeEmptyRows();
    wFilter.setRowNums();
    wFilter.optWidth(true);

    if (meta.additionalOutputFields.shortFilenameField != null) {
      wShortFileFieldName.setText(meta.additionalOutputFields.shortFilenameField);
    }
    if (meta.additionalOutputFields.pathField != null) {
      wPathFieldName.setText(meta.additionalOutputFields.pathField);
    }
    if (meta.additionalOutputFields.hiddenField != null) {
      wIsHiddenName.setText(meta.additionalOutputFields.hiddenField);
    }
    if (meta.additionalOutputFields.lastModificationField != null) {
      wLastModificationTimeName.setText(meta.additionalOutputFields.lastModificationField);
    }
    if (meta.additionalOutputFields.uriField != null) {
      wUriName.setText(meta.additionalOutputFields.uriField);
    }
    if (meta.additionalOutputFields.rootUriField != null) {
      wRootUriName.setText(meta.additionalOutputFields.rootUriField);
    }
    if (meta.additionalOutputFields.extensionField != null) {
      wExtensionFieldName.setText(meta.additionalOutputFields.extensionField);
    }
    if (meta.additionalOutputFields.sizeField != null) {
      wSizeFieldName.setText(meta.additionalOutputFields.sizeField);
    }

    setFlags();

    wTransformName.selectAll();
    wTransformName.setFocus();
  }

  private void getFieldsData(
      TextFileInputMeta in,
      boolean insertAtTop,
      final boolean reloadAllFields,
      final List<String> newFieldNames) {
    final List<String> lowerCaseNewFieldNames =
        newFieldNames == null ? new ArrayList() : newFieldNames;
    for (int i = 0; i < in.inputFields.length; i++) {
      BaseFileField field = in.inputFields[i];

      TableItem item;

      if (insertAtTop) {
        item = new TableItem(wFields.table, SWT.NONE, i);
      } else {
        item = getTableItem(field.getName(), true);
      }
      if (!reloadAllFields && !lowerCaseNewFieldNames.contains(field.getName().toLowerCase())) {
        continue;
      }

      item.setText(1, Const.NVL(field.getName(), ""));
      String type = field.getTypeDesc();
      String format = field.getFormat();
      String position = "" + field.getPosition();
      String length = "" + field.getLength();
      String prec = "" + field.getPrecision();
      String curr = field.getCurrencySymbol();
      String group = field.getGroupSymbol();
      String decim = field.getDecimalSymbol();
      String def = field.getNullString();
      String ifNull = field.getIfNullValue();
      String trim = field.getTrimTypeDesc();
      String rep =
          field.isRepeated()
              ? BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_YES)
              : BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_NO);

      if (type != null) {
        item.setText(2, type);
      }
      if (format != null) {
        item.setText(3, format);
      }
      if (position != null && !"-1".equals(position)) {
        item.setText(4, position);
      }
      if (length != null && !"-1".equals(length)) {
        item.setText(5, length);
      }
      if (prec != null && !"-1".equals(prec)) {
        item.setText(6, prec);
      }
      if (curr != null) {
        item.setText(7, curr);
      }
      if (decim != null) {
        item.setText(8, decim);
      }
      if (group != null) {
        item.setText(9, group);
      }
      if (def != null) {
        item.setText(10, def);
      }
      if (ifNull != null) {
        item.setText(11, ifNull);
      }
      if (trim != null) {
        item.setText(12, trim);
      }
      if (rep != null) {
        item.setText(13, rep);
      }
    }
  }

  private void setEncodings() {
    // Encoding of the text file:
    if (!gotEncodings) {
      gotEncodings = true;

      wEncoding.removeAll();
      List<Charset> values = new ArrayList<>(Charset.availableCharsets().values());
      for (Charset charSet : values) {
        wEncoding.add(charSet.displayName());
      }

      // Now select the default!
      String defEncoding = Const.getEnvironmentVariable("file.encoding", "UTF-8");
      int idx = Const.indexOfString(defEncoding, wEncoding.getItems());
      if (idx >= 0) {
        wEncoding.select(idx);
      }
    }
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

    getInfo(input, false);
    dispose();
  }

  /**
   * Fill meta object from UI options.
   *
   * @param meta meta object
   * @param preview flag for preview or real options should be used. Currently, only one option is
   *     differ for preview - EOL chars. It uses as "mixed" for be able to preview any file.
   */
  private void getInfo(TextFileInputMeta meta, boolean preview) {
    transformName = wTransformName.getText(); // return value

    // copy info to TextFileInputMeta class (input)
    meta.inputFiles.acceptingFilenames = wAccFilenames.getSelection();
    meta.inputFiles.passingThruFields = wPassThruFields.getSelection();
    meta.inputFiles.acceptingField = wAccField.getText();
    meta.inputFiles.acceptingTransformName = wAccTransform.getText();
    meta.setAcceptingTransform(pipelineMeta.findTransform(wAccTransform.getText()));

    meta.content.fileType = wFiletype.getText();
    if (preview) {
      // mixed type for preview, for be able to eat any EOL chars
      meta.content.fileFormat = "mixed";
    } else {
      meta.content.fileFormat = wFormat.getText();
    }
    meta.content.separator = wSeparator.getText();
    meta.content.enclosure = wEnclosure.getText();
    meta.content.escapeCharacter = wEscape.getText();
    meta.content.breakInEnclosureAllowed = wEnclBreaks.getSelection();
    meta.content.rowLimit = Const.toLong(wLimit.getText(), 0L);
    meta.content.filenameField = wInclFilenameField.getText();
    meta.content.rowNumberField = wInclRownumField.getText();
    meta.inputFiles.isaddresult = wAddResult.getSelection();

    meta.content.includeFilename = wInclFilename.getSelection();
    meta.content.includeRowNumber = wInclRownum.getSelection();
    meta.content.rowNumberByFile = wRownumByFile.getSelection();
    meta.content.prependFileName = wPrependFileName.getSelection();
    meta.content.header = wHeader.getSelection();
    meta.content.nrHeaderLines = Const.toInt(wNrHeader.getText(), 1);
    meta.content.footer = wFooter.getSelection();
    meta.content.nrFooterLines = Const.toInt(wNrFooter.getText(), 1);
    meta.content.lineWrapped = wWraps.getSelection();
    meta.content.nrWraps = Const.toInt(wNrWraps.getText(), 1);
    meta.content.layoutPaged = wLayoutPaged.getSelection();
    meta.content.nrLinesPerPage = Const.toInt(wNrLinesPerPage.getText(), 80);
    meta.content.nrLinesDocHeader = Const.toInt(wNrLinesDocHeader.getText(), 0);
    meta.content.fileCompression = wCompression.getText();
    meta.content.dateFormatLenient = wDateLenient.getSelection();
    meta.content.noEmptyLines = wNoempty.getSelection();
    meta.content.encoding = wEncoding.getText();
    meta.content.length = wLength.getText();

    int nrfiles = wFilenameList.getItemCount();
    int nrFields = wFields.nrNonEmpty();
    int nrfilters = wFilter.nrNonEmpty();
    meta.allocate(nrfiles, nrFields, nrfilters);

    meta.setFileName(wFilenameList.getItems(0));
    meta.setSchemaDefinition(wSchemaDefinition.getText());
    meta.inputFiles.fileMask = wFilenameList.getItems(1);
    meta.inputFiles.excludeFileMask = wFilenameList.getItems(2);
    meta.inputFiles_fileRequired(wFilenameList.getItems(3));
    meta.inputFiles_includeSubFolders(wFilenameList.getItems(4));

    for (int i = 0; i < nrFields; i++) {
      BaseFileField field = new BaseFileField();

      TableItem item = wFields.getNonEmpty(i);
      field.setName(item.getText(1));
      field.setType(ValueMetaFactory.getIdForValueMeta(item.getText(2)));
      field.setFormat(item.getText(3));
      field.setPosition(Const.toInt(item.getText(4), -1));
      field.setLength(Const.toInt(item.getText(5), -1));
      field.setPrecision(Const.toInt(item.getText(6), -1));
      field.setCurrencySymbol(item.getText(7));
      field.setDecimalSymbol(item.getText(8));
      field.setGroupSymbol(item.getText(9));
      field.setNullString(item.getText(10));
      field.setIfNullValue(item.getText(11));
      field.setTrimType(ValueMetaBase.getTrimTypeByDesc(item.getText(12)));
      field.setRepeated(
          BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_YES).equalsIgnoreCase(item.getText(13)));

      meta.inputFields[i] = field;
    }

    for (int i = 0; i < nrfilters; i++) {
      TableItem item = wFilter.getNonEmpty(i);
      TextFileFilter filter = new TextFileFilter();

      meta.getFilter()[i] = filter;

      filter.setFilterString(item.getText(1));
      filter.setFilterPosition(Const.toInt(item.getText(2), -1));
      filter.setFilterLastLine(
          BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_YES).equalsIgnoreCase(item.getText(3)));
      filter.setFilterPositive(
          BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_YES).equalsIgnoreCase(item.getText(4)));
    }
    // Error handling fields...
    meta.errorHandling.errorIgnored = wErrorIgnored.getSelection();
    meta.errorHandling.skipBadFiles = wSkipBadFiles.getSelection();
    meta.errorHandling.fileErrorField = wBadFileField.getText();
    meta.errorHandling.fileErrorMessageField = wBadFileMessageField.getText();
    meta.setErrorLineSkipped(wSkipErrorLines.getSelection());
    meta.setErrorCountField(wErrorCount.getText());
    meta.setErrorFieldsField(wErrorFields.getText());
    meta.setErrorTextField(wErrorText.getText());

    meta.errorHandling.warningFilesDestinationDirectory = wWarnDestDir.getText();
    meta.errorHandling.warningFilesExtension = wWarnExt.getText();
    meta.errorHandling.errorFilesDestinationDirectory = wErrorDestDir.getText();
    meta.errorHandling.errorFilesExtension = wErrorExt.getText();
    meta.errorHandling.lineNumberFilesDestinationDirectory = wLineNrDestDir.getText();
    meta.errorHandling.lineNumberFilesExtension = wLineNrExt.getText();

    // Date format Locale
    Locale locale = EnvUtil.createLocale(wDateLocale.getText());
    if (!locale.equals(Locale.getDefault())) {
      meta.content.dateFormatLocale = locale;
    } else {
      meta.content.dateFormatLocale = Locale.getDefault();
    }

    meta.additionalOutputFields.shortFilenameField = wShortFileFieldName.getText();
    meta.additionalOutputFields.pathField = wPathFieldName.getText();
    meta.additionalOutputFields.hiddenField = wIsHiddenName.getText();
    meta.additionalOutputFields.lastModificationField = wLastModificationTimeName.getText();
    meta.additionalOutputFields.uriField = wUriName.getText();
    meta.additionalOutputFields.rootUriField = wRootUriName.getText();
    meta.additionalOutputFields.extensionField = wExtensionFieldName.getText();
    meta.additionalOutputFields.sizeField = wSizeFieldName.getText();
  }

  private void get() {
    if (wFiletype.getText().equalsIgnoreCase("CSV")) {
      getFields();
    }
  }

  @Override
  public String loadFieldsImpl(final TextFileInputMeta meta, final int samples) {
    return loadFieldsImpl((ICsvInputAwareMeta) meta, samples);
  }

  @Override
  public String massageFieldName(final String fieldName) {
    // Replace all spaces and hyphens (-) with underscores (_)
    String massagedFieldName = fieldName;
    massagedFieldName = Const.replace(massagedFieldName, " ", "_");
    massagedFieldName = Const.replace(massagedFieldName, "-", "_");
    return massagedFieldName;
  }

  @Override
  public String[] getFieldNames(final TextFileInputMeta meta) {
    return getFieldNames((ICsvInputAwareMeta) meta);
  }

  public static int guessPrecision(double d) {
    // Round numbers
    long frac = Math.round((d - Math.floor(d)) * 1E10); // max precision : 10
    int precision = 10;

    // 0,34 --> 3400000000
    // 0 to the right --> precision -1!
    // 0 to the right means frac%10 == 0

    while (precision >= 0 && (frac % 10) == 0) {
      frac /= 10;
      precision--;
    }
    precision++;

    return precision;
  }

  public static int guessIntLength(double d) {
    double flr = Math.floor(d);
    int len = 1;

    while (flr > 9) {
      flr /= 10;
      flr = Math.floor(flr);
      len++;
    }

    return len;
  }

  // Preview the data
  private void preview() {
    // Create the XML input transform
    TextFileInputMeta oneMeta = new TextFileInputMeta();
    getInfo(oneMeta, true);

    if (oneMeta.inputFiles.acceptingFilenames) {
      MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION);
      mb.setMessage(
          BaseMessages.getString(PKG, "TextFileInputDialog.Dialog.SpecifyASampleFile.Message"));
      mb.setText(
          BaseMessages.getString(PKG, "TextFileInputDialog.Dialog.SpecifyASampleFile.Title"));
      mb.open();
      return;
    }

    PipelineMeta previewMeta =
        PipelinePreviewFactory.generatePreviewPipeline(
            pipelineMeta.getMetadataProvider(), oneMeta, wTransformName.getText());

    EnterNumberDialog numberDialog =
        new EnterNumberDialog(
            shell,
            props.getDefaultPreviewSize(),
            BaseMessages.getString(PKG, "TextFileInputDialog.PreviewSize.DialogTitle"),
            BaseMessages.getString(PKG, "TextFileInputDialog.PreviewSize.DialogMessage"));
    int previewSize = numberDialog.open();
    if (previewSize > 0) {
      PipelinePreviewProgressDialog progressDialog =
          new PipelinePreviewProgressDialog(
              shell,
              variables,
              previewMeta,
              new String[] {wTransformName.getText()},
              new int[] {previewSize});
      progressDialog.open();

      Pipeline pipeline = progressDialog.getPipeline();
      String loggingText = progressDialog.getLoggingText();

      if (!progressDialog.isCancelled()
          && pipeline.getResult() != null
          && pipeline.getResult().getNrErrors() > 0) {
        EnterTextDialog etd =
            new EnterTextDialog(
                shell,
                BaseMessages.getString(PKG, "System.Dialog.PreviewError.Title"),
                BaseMessages.getString(PKG, "System.Dialog.PreviewError.Message"),
                loggingText,
                true);
        etd.setReadOnly();
        etd.open();
      }

      PreviewRowsDialog prd =
          new PreviewRowsDialog(
              shell,
              variables,
              SWT.NONE,
              wTransformName.getText(),
              progressDialog.getPreviewRowsMeta(wTransformName.getText()),
              progressDialog.getPreviewRows(wTransformName.getText()),
              loggingText);
      prd.open();
    }
  }

  // Get the first x lines
  private void first(boolean skipHeaders) {
    TextFileInputMeta info = new TextFileInputMeta();
    getInfo(info, true);

    try {
      if (info.getFileInputList(variables).nrOfFiles() > 0) {
        String shellText =
            BaseMessages.getString(PKG, "TextFileInputDialog.LinesToView.DialogTitle");
        String lineText =
            BaseMessages.getString(PKG, "TextFileInputDialog.LinesToView.DialogMessage");
        EnterNumberDialog end = new EnterNumberDialog(shell, 100, shellText, lineText);
        EnterNumberDialogResult result = end.openWithFromTo();
        if (result.getNumberOfLines() >= 0 || result.getFromLine() >= 0) {
          List<String> linesList =
              getFirst(
                  result.getNumberOfLines(), result.getFromLine(), result.getToLine(), skipHeaders);
          if (!linesList.isEmpty()) {
            StringBuilder firstlines = new StringBuilder();
            for (String aLinesList : linesList) {
              firstlines.append(aLinesList).append(Const.CR);
            }

            String message;
            if (result.getNumberOfLines() == -1 && result.getToLine() > 0) {
              message =
                  BaseMessages.getString(
                      PKG,
                      "TextFileInputDialog.ContentOfFirstFile.NLinesFromTo.DialogMessage",
                      result.getFromLine(),
                      result.getToLine());
            } else if (result.getNumberOfLines() > 0 && result.getFromLine() == 0) {
              message =
                  BaseMessages.getString(
                      PKG,
                      "TextFileInputDialog.ContentOfFirstFile.NLines.DialogMessage",
                      result.getNumberOfLines());
            } else {
              message =
                  BaseMessages.getString(
                      PKG, "TextFileInputDialog.ContentOfFirstFile.AllLines.DialogMessage");
            }

            EnterTextDialog etd =
                new EnterTextDialog(
                    shell,
                    BaseMessages.getString(
                        PKG, "TextFileInputDialog.ContentOfFirstFile.DialogTitle"),
                    message,
                    firstlines.toString(),
                    true);
            etd.setReadOnly();
            etd.open();
          } else {
            MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
            mb.setMessage(
                BaseMessages.getString(PKG, "TextFileInputDialog.UnableToReadLines.DialogMessage"));
            mb.setText(
                BaseMessages.getString(PKG, "TextFileInputDialog.UnableToReadLines.DialogTitle"));
            mb.open();
          }
        }
      } else {
        MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
        mb.setMessage(BaseMessages.getString(PKG, "TextFileInputDialog.NoValidFile.DialogMessage"));
        mb.setText(BaseMessages.getString(PKG, CONST_SYSTEM_DIALOG_ERROR_TITLE));
        mb.open();
      }
    } catch (HopException e) {
      displayErrorDialog(e, "TextFileInputDialog.ErrorGettingData.DialogMessage");
    }
  }

  private void displayErrorDialog(HopException e, String messageKey) {
    new ErrorDialog(
        shell,
        BaseMessages.getString(PKG, CONST_SYSTEM_DIALOG_ERROR_TITLE),
        BaseMessages.getString(PKG, messageKey),
        e);
  }

  // Get the first x lines
  private List<String> getFirst(int nrLines, int fromLine, int toLine, boolean skipHeaders)
      throws HopException {
    TextFileInputMeta meta = new TextFileInputMeta();
    getInfo(meta, true);
    FileInputList textFileList = meta.getFileInputList(variables);

    InputStream fi;
    CompressionInputStream f = null;
    StringBuilder lineStringBuilder = new StringBuilder(256);
    int fileFormatType = meta.getFileFormatTypeNr();

    List<String> retval = new ArrayList<>();

    if (textFileList.nrOfFiles() > 0) {
      FileObject file = textFileList.getFile(0);
      try {
        fi = HopVfs.getInputStream(file);

        ICompressionProvider provider =
            CompressionProviderFactory.getInstance()
                .createCompressionProviderInstance(meta.content.fileCompression);
        f = provider.createInputStream(fi);

        InputStreamReader reader;
        if (meta.getEncoding() != null && !meta.getEncoding().isEmpty()) {
          reader = new InputStreamReader(f, meta.getEncoding());
        } else {
          reader = new InputStreamReader(f);
        }
        EncodingType encodingType = EncodingType.guessEncodingType(reader.getEncoding());

        int lineNr = 0;

        // change nrLines when toLine is filled in
        nrLines = (toLine > 0) ? toLine : nrLines;

        int maxNr = nrLines + (meta.content.header ? meta.content.nrHeaderLines : 0);

        if (skipHeaders) {
          // Skip the header lines first if more then one, it helps us position
          if (meta.content.layoutPaged && meta.content.nrLinesDocHeader > 0) {
            int skipped = 0;
            String line =
                TextFileLineUtil.getLine(
                    log,
                    reader,
                    encodingType,
                    fileFormatType,
                    lineStringBuilder,
                    meta.getEnclosure(),
                    meta.getEscapeCharacter(),
                    meta.isBreakInEnclosureAllowed());
            while (line != null && skipped < meta.content.nrLinesDocHeader - 1) {
              skipped++;
              line =
                  TextFileLineUtil.getLine(
                      log,
                      reader,
                      encodingType,
                      fileFormatType,
                      lineStringBuilder,
                      meta.getEnclosure(),
                      meta.getEscapeCharacter(),
                      meta.isBreakInEnclosureAllowed());
            }
          }

          // Skip the header lines first if more then one, it helps us position
          if (meta.content.header && meta.content.nrHeaderLines > 0) {
            int skipped = 0;
            String line =
                TextFileLineUtil.getLine(
                    log,
                    reader,
                    encodingType,
                    fileFormatType,
                    lineStringBuilder,
                    meta.getEnclosure(),
                    meta.getEscapeCharacter(),
                    meta.isBreakInEnclosureAllowed());
            while (line != null && skipped < meta.content.nrHeaderLines - 1) {
              skipped++;
              line =
                  TextFileLineUtil.getLine(
                      log,
                      reader,
                      encodingType,
                      fileFormatType,
                      lineStringBuilder,
                      meta.getEnclosure(),
                      meta.getEscapeCharacter(),
                      meta.isBreakInEnclosureAllowed());
            }
          }
        }

        String line =
            TextFileLineUtil.getLine(
                log,
                reader,
                encodingType,
                fileFormatType,
                lineStringBuilder,
                meta.getEnclosure(),
                meta.getEscapeCharacter(),
                meta.isBreakInEnclosureAllowed());
        while (line != null && (lineNr < maxNr || nrLines == 0)) {
          if (lineNr >= fromLine) {
            retval.add(line);
          }
          lineNr++;
          line =
              TextFileLineUtil.getLine(
                  log,
                  reader,
                  encodingType,
                  fileFormatType,
                  lineStringBuilder,
                  meta.getEnclosure(),
                  meta.getEscapeCharacter(),
                  meta.isBreakInEnclosureAllowed());
        }
      } catch (Exception e) {
        throw new HopException(
            BaseMessages.getString(
                PKG,
                "TextFileInputDialog.Exception.ErrorGettingFirstLines",
                "" + nrLines,
                file.getName().getURI()),
            e);
      } finally {
        try {
          if (f != null) {
            f.close();
          }
        } catch (Exception e) {
          // Ignore errors
        }
      }
    }

    return retval;
  }

  private Vector<ITextFileInputField> getFields(TextFileInputMeta info, List<String> rows) {
    Vector<ITextFileInputField> fields = new Vector<>();

    int maxsize = 0;
    for (String row : rows) {
      int len = row.length();
      if (len > maxsize) {
        maxsize = len;
      }
    }

    int prevEnd = 0;
    int dummynr = 1;

    for (int i = 0; i < info.inputFields.length; i++) {
      BaseFileField f = info.inputFields[i];

      // See if positions are skipped, if this is the case, add dummy fields...
      if (f.getPosition() != prevEnd) { // gap

        BaseFileField field =
            new BaseFileField("Dummy" + dummynr, prevEnd, f.getPosition() - prevEnd);
        field.setIgnored(true); // don't include in result by default.
        fields.add(field);
        dummynr++;
      }

      BaseFileField field = new BaseFileField(f.getName(), f.getPosition(), f.getLength());
      field.setType(f.getType());
      field.setIgnored(false);
      field.setFormat(f.getFormat());
      field.setPrecision(f.getPrecision());
      field.setTrimType(f.getTrimType());
      field.setDecimalSymbol(f.getDecimalSymbol());
      field.setGroupSymbol(f.getGroupSymbol());
      field.setCurrencySymbol(f.getCurrencySymbol());
      field.setRepeated(f.isRepeated());
      field.setNullString(f.getNullString());

      fields.add(field);

      prevEnd = field.getPosition() + field.getLength();
    }

    if (info.inputFields.length == 0) {
      BaseFileField field = new BaseFileField("Field1", 0, maxsize);
      fields.add(field);
    } else {
      // Take the last field and see if it reached until the maximum...
      BaseFileField f = info.inputFields[info.inputFields.length - 1];

      int pos = f.getPosition();
      int len = f.getLength();
      if (pos + len < maxsize) {
        // If not, add an extra trailing field!
        BaseFileField field = new BaseFileField("Dummy" + dummynr, pos + len, maxsize - pos - len);
        field.setIgnored(true); // don't include in result by default.
        fields.add(field);
      }
    }

    Collections.sort(fields);

    return fields;
  }

  /** Sets the input width to minimal width... */
  public void setMinimalWidth() {
    int nrNonEmptyFields = wFields.nrNonEmpty();
    for (int i = 0; i < nrNonEmptyFields; i++) {
      TableItem item = wFields.getNonEmpty(i);

      item.setText(5, "");
      item.setText(6, "");
      item.setText(12, ValueMetaBase.getTrimTypeDesc(IValueMeta.TRIM_TYPE_BOTH));

      int type = ValueMetaFactory.getIdForValueMeta(item.getText(2));
      switch (type) {
        case IValueMeta.TYPE_STRING:
          item.setText(3, "");
          break;
        case IValueMeta.TYPE_INTEGER:
          item.setText(3, "0");
          break;
        case IValueMeta.TYPE_NUMBER:
          item.setText(3, "0.#####");
          break;
        case IValueMeta.TYPE_DATE:
          break;
        default:
          break;
      }
    }

    for (int i = 0; i < input.inputFields.length; i++) {
      input.inputFields[i].setTrimType(IValueMeta.TRIM_TYPE_BOTH);
    }

    wFields.optWidth(true);
  }

  /**
   * Overloading setMinimalWidth() in order to test trim functionality
   *
   * @param wFields mocked TableView to avoid wFields.nrNonEmpty() from throwing
   *     NullPointerException
   */
  public void setMinimalWidth(TableView wFields) {
    this.wFields = wFields;
    this.setMinimalWidth();
  }

  private void addAdditionalFieldsTab() {
    // ////////////////////////
    // START OF ADDITIONAL FIELDS TAB ///
    // ////////////////////////
    CTabItem wAdditionalFieldsTab = new CTabItem(wTabFolder, SWT.NONE);
    wAdditionalFieldsTab.setFont(GuiResource.getInstance().getFontDefault());
    wAdditionalFieldsTab.setText(
        BaseMessages.getString(PKG, "TextFileInputDialog.AdditionalFieldsTab.TabTitle"));

    Composite wAdditionalFieldsComp = new Composite(wTabFolder, SWT.NONE);
    PropsUi.setLook(wAdditionalFieldsComp);

    FormLayout fieldsLayout = new FormLayout();
    fieldsLayout.marginWidth = 3;
    fieldsLayout.marginHeight = 3;
    wAdditionalFieldsComp.setLayout(fieldsLayout);

    // ShortFileFieldName line
    Label wlShortFileFieldName = new Label(wAdditionalFieldsComp, SWT.RIGHT);
    wlShortFileFieldName.setText(
        BaseMessages.getString(PKG, "TextFileInputDialog.ShortFileFieldName.Label"));
    PropsUi.setLook(wlShortFileFieldName);
    FormData fdlShortFileFieldName = new FormData();
    fdlShortFileFieldName.left = new FormAttachment(0, 0);
    fdlShortFileFieldName.top = new FormAttachment(margin, margin);
    fdlShortFileFieldName.right = new FormAttachment(middle, -margin);
    wlShortFileFieldName.setLayoutData(fdlShortFileFieldName);

    wShortFileFieldName =
        new TextVar(variables, wAdditionalFieldsComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wShortFileFieldName);
    wShortFileFieldName.addModifyListener(lsMod);
    FormData fdShortFileFieldName = new FormData();
    fdShortFileFieldName.left = new FormAttachment(middle, 0);
    fdShortFileFieldName.right = new FormAttachment(100, -margin);
    fdShortFileFieldName.top = new FormAttachment(margin, margin);
    wShortFileFieldName.setLayoutData(fdShortFileFieldName);

    // ExtensionFieldName line
    Label wlExtensionFieldName = new Label(wAdditionalFieldsComp, SWT.RIGHT);
    wlExtensionFieldName.setText(
        BaseMessages.getString(PKG, "TextFileInputDialog.ExtensionFieldName.Label"));
    PropsUi.setLook(wlExtensionFieldName);
    FormData fdlExtensionFieldName = new FormData();
    fdlExtensionFieldName.left = new FormAttachment(0, 0);
    fdlExtensionFieldName.top = new FormAttachment(wShortFileFieldName, margin);
    fdlExtensionFieldName.right = new FormAttachment(middle, -margin);
    wlExtensionFieldName.setLayoutData(fdlExtensionFieldName);

    wExtensionFieldName =
        new TextVar(variables, wAdditionalFieldsComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wExtensionFieldName);
    wExtensionFieldName.addModifyListener(lsMod);
    FormData fdExtensionFieldName = new FormData();
    fdExtensionFieldName.left = new FormAttachment(middle, 0);
    fdExtensionFieldName.right = new FormAttachment(100, -margin);
    fdExtensionFieldName.top = new FormAttachment(wShortFileFieldName, margin);
    wExtensionFieldName.setLayoutData(fdExtensionFieldName);

    // PathFieldName line
    Label wlPathFieldName = new Label(wAdditionalFieldsComp, SWT.RIGHT);
    wlPathFieldName.setText(BaseMessages.getString(PKG, "TextFileInputDialog.PathFieldName.Label"));
    PropsUi.setLook(wlPathFieldName);
    FormData fdlPathFieldName = new FormData();
    fdlPathFieldName.left = new FormAttachment(0, 0);
    fdlPathFieldName.top = new FormAttachment(wExtensionFieldName, margin);
    fdlPathFieldName.right = new FormAttachment(middle, -margin);
    wlPathFieldName.setLayoutData(fdlPathFieldName);

    wPathFieldName =
        new TextVar(variables, wAdditionalFieldsComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wPathFieldName);
    wPathFieldName.addModifyListener(lsMod);
    FormData fdPathFieldName = new FormData();
    fdPathFieldName.left = new FormAttachment(middle, 0);
    fdPathFieldName.right = new FormAttachment(100, -margin);
    fdPathFieldName.top = new FormAttachment(wExtensionFieldName, margin);
    wPathFieldName.setLayoutData(fdPathFieldName);

    // SizeFieldName line
    Label wlSizeFieldName = new Label(wAdditionalFieldsComp, SWT.RIGHT);
    wlSizeFieldName.setText(BaseMessages.getString(PKG, "TextFileInputDialog.SizeFieldName.Label"));
    PropsUi.setLook(wlSizeFieldName);
    FormData fdlSizeFieldName = new FormData();
    fdlSizeFieldName.left = new FormAttachment(0, 0);
    fdlSizeFieldName.top = new FormAttachment(wPathFieldName, margin);
    fdlSizeFieldName.right = new FormAttachment(middle, -margin);
    wlSizeFieldName.setLayoutData(fdlSizeFieldName);

    wSizeFieldName =
        new TextVar(variables, wAdditionalFieldsComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wSizeFieldName);
    wSizeFieldName.addModifyListener(lsMod);
    FormData fdSizeFieldName = new FormData();
    fdSizeFieldName.left = new FormAttachment(middle, 0);
    fdSizeFieldName.right = new FormAttachment(100, -margin);
    fdSizeFieldName.top = new FormAttachment(wPathFieldName, margin);
    wSizeFieldName.setLayoutData(fdSizeFieldName);

    // IsHiddenName line
    Label wlIsHiddenName = new Label(wAdditionalFieldsComp, SWT.RIGHT);
    wlIsHiddenName.setText(BaseMessages.getString(PKG, "TextFileInputDialog.IsHiddenName.Label"));
    PropsUi.setLook(wlIsHiddenName);
    FormData fdlIsHiddenName = new FormData();
    fdlIsHiddenName.left = new FormAttachment(0, 0);
    fdlIsHiddenName.top = new FormAttachment(wSizeFieldName, margin);
    fdlIsHiddenName.right = new FormAttachment(middle, -margin);
    wlIsHiddenName.setLayoutData(fdlIsHiddenName);

    wIsHiddenName =
        new TextVar(variables, wAdditionalFieldsComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wIsHiddenName);
    wIsHiddenName.addModifyListener(lsMod);
    FormData fdIsHiddenName = new FormData();
    fdIsHiddenName.left = new FormAttachment(middle, 0);
    fdIsHiddenName.right = new FormAttachment(100, -margin);
    fdIsHiddenName.top = new FormAttachment(wSizeFieldName, margin);
    wIsHiddenName.setLayoutData(fdIsHiddenName);

    // LastModificationTimeName line
    Label wlLastModificationTimeName = new Label(wAdditionalFieldsComp, SWT.RIGHT);
    wlLastModificationTimeName.setText(
        BaseMessages.getString(PKG, "TextFileInputDialog.LastModificationTimeName.Label"));
    PropsUi.setLook(wlLastModificationTimeName);
    FormData fdlLastModificationTimeName = new FormData();
    fdlLastModificationTimeName.left = new FormAttachment(0, 0);
    fdlLastModificationTimeName.top = new FormAttachment(wIsHiddenName, margin);
    fdlLastModificationTimeName.right = new FormAttachment(middle, -margin);
    wlLastModificationTimeName.setLayoutData(fdlLastModificationTimeName);

    wLastModificationTimeName =
        new TextVar(variables, wAdditionalFieldsComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wLastModificationTimeName);
    wLastModificationTimeName.addModifyListener(lsMod);
    FormData fdLastModificationTimeName = new FormData();
    fdLastModificationTimeName.left = new FormAttachment(middle, 0);
    fdLastModificationTimeName.right = new FormAttachment(100, -margin);
    fdLastModificationTimeName.top = new FormAttachment(wIsHiddenName, margin);
    wLastModificationTimeName.setLayoutData(fdLastModificationTimeName);

    // UriName line
    Label wlUriName = new Label(wAdditionalFieldsComp, SWT.RIGHT);
    wlUriName.setText(BaseMessages.getString(PKG, "TextFileInputDialog.UriName.Label"));
    PropsUi.setLook(wlUriName);
    FormData fdlUriName = new FormData();
    fdlUriName.left = new FormAttachment(0, 0);
    fdlUriName.top = new FormAttachment(wLastModificationTimeName, margin);
    fdlUriName.right = new FormAttachment(middle, -margin);
    wlUriName.setLayoutData(fdlUriName);

    wUriName = new TextVar(variables, wAdditionalFieldsComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wUriName);
    wUriName.addModifyListener(lsMod);
    FormData fdUriName = new FormData();
    fdUriName.left = new FormAttachment(middle, 0);
    fdUriName.right = new FormAttachment(100, -margin);
    fdUriName.top = new FormAttachment(wLastModificationTimeName, margin);
    wUriName.setLayoutData(fdUriName);

    // RootUriName line
    Label wlRootUriName = new Label(wAdditionalFieldsComp, SWT.RIGHT);
    wlRootUriName.setText(BaseMessages.getString(PKG, "TextFileInputDialog.RootUriName.Label"));
    PropsUi.setLook(wlRootUriName);
    FormData fdlRootUriName = new FormData();
    fdlRootUriName.left = new FormAttachment(0, 0);
    fdlRootUriName.top = new FormAttachment(wUriName, margin);
    fdlRootUriName.right = new FormAttachment(middle, -margin);
    wlRootUriName.setLayoutData(fdlRootUriName);

    wRootUriName =
        new TextVar(variables, wAdditionalFieldsComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wRootUriName);
    wRootUriName.addModifyListener(lsMod);
    FormData fdRootUriName = new FormData();
    fdRootUriName.left = new FormAttachment(middle, 0);
    fdRootUriName.right = new FormAttachment(100, -margin);
    fdRootUriName.top = new FormAttachment(wUriName, margin);
    wRootUriName.setLayoutData(fdRootUriName);

    FormData fdAdditionalFieldsComp = new FormData();
    fdAdditionalFieldsComp.left = new FormAttachment(0, 0);
    fdAdditionalFieldsComp.top = new FormAttachment(0, 0);
    fdAdditionalFieldsComp.right = new FormAttachment(100, 0);
    fdAdditionalFieldsComp.bottom = new FormAttachment(100, 0);
    wAdditionalFieldsComp.setLayoutData(fdAdditionalFieldsComp);

    wAdditionalFieldsComp.layout();
    wAdditionalFieldsTab.setControl(wAdditionalFieldsComp);

    // ///////////////////////////////////////////////////////////
    // / END OF ADDITIONAL FIELDS TAB
    // ///////////////////////////////////////////////////////////

  }

  @Override
  public TableView getFieldsTable() {
    return this.wFields;
  }

  @Override
  public Shell getShell() {
    return this.shell;
  }

  @Override
  public TextFileInputMeta getNewMetaInstance() {
    return new TextFileInputMeta();
  }

  @Override
  public void populateMeta(TextFileInputMeta inputMeta) {
    getInfo(inputMeta, false);
  }

  @Override
  public ICsvInputAwareImportProgressDialog getCsvImportProgressDialog(
      final ICsvInputAwareMeta meta, final int samples, final InputStreamReader reader) {
    return new TextFileCSVImportProgressDialog(
        getShell(), variables, (TextFileInputMeta) meta, pipelineMeta, reader, samples, true);
  }

  @Override
  public LogChannel getLogChannel() {
    return log;
  }

  @Override
  public PipelineMeta getPipelineMeta() {
    return pipelineMeta;
  }

  @Override
  public InputStream getInputStream(final ICsvInputAwareMeta meta) {
    InputStream fileInputStream;
    CompressionInputStream inputStream = null;
    try {
      FileObject fileObject = meta.getHeaderFileObject(variables);
      fileInputStream = HopVfs.getInputStream(fileObject);
      ICompressionProvider provider =
          CompressionProviderFactory.getInstance()
              .createCompressionProviderInstance(
                  ((TextFileInputMeta) meta).content.fileCompression);
      inputStream = provider.createInputStream(fileInputStream);
    } catch (final Exception e) {
      logError(BaseMessages.getString("FileInputDialog.ErrorGettingFileDesc.DialogMessage"), e);
    }
    return inputStream;
  }
}
