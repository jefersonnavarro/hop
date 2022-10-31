/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.ui.hopgui.perspective.configuration;

import org.apache.hop.core.Props;
import org.apache.hop.core.gui.plugin.GuiPlugin;
import org.apache.hop.core.gui.plugin.GuiRegistry;
import org.apache.hop.core.gui.plugin.key.GuiKeyboardShortcut;
import org.apache.hop.core.gui.plugin.key.GuiOsxKeyboardShortcut;
import org.apache.hop.core.gui.plugin.tab.GuiTabItem;
import org.apache.hop.core.search.ISearchable;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.gui.GuiResource;
import org.apache.hop.ui.hopgui.HopGui;
import org.apache.hop.ui.hopgui.context.IGuiContextHandler;
import org.apache.hop.ui.hopgui.file.IHopFileType;
import org.apache.hop.ui.hopgui.file.IHopFileTypeHandler;
import org.apache.hop.ui.hopgui.file.empty.EmptyHopFileTypeHandler;
import org.apache.hop.ui.hopgui.perspective.HopPerspectivePlugin;
import org.apache.hop.ui.hopgui.perspective.IHopPerspective;
import org.apache.hop.ui.hopgui.perspective.TabClosable;
import org.apache.hop.ui.hopgui.perspective.TabItemHandler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@HopPerspectivePlugin(
        id = "160-HopConfigurationPerspective",
        name = "i18n::ConfigurationPerspective.Name",
        description = "i18n::ConfigurationPerspective.Description",
        image = "ui/images/gear.svg"
)
@GuiPlugin(description = "i18n::HopConfigurationPerspective.GuiPlugin.Description")
public class ConfigurationPerspective implements IHopPerspective, TabClosable {

    private static final Class<?> PKG = ConfigurationPerspective.class;

    public static final String CONFIG_PERSPECTIVE_TABS = "ConfigurationPerspective.Tabs.ID";
    private HopGui hopGui;
    private Composite composite;
    public CTabFolder configTabs;
    private static ConfigurationPerspective instance;
    public static ConfigurationPerspective getInstance(){
        return instance;
    }

    public ConfigurationPerspective(){
        instance = this;
    }

    @Override
    public List<IGuiContextHandler> getContextHandlers() {
        List<IGuiContextHandler> handlers = new ArrayList<>();
        return handlers;
    }

    @Override
    public String getId() {
        return "configuration";
    }

    @Override
    public IHopFileTypeHandler getActiveFileTypeHandler() {
        return new EmptyHopFileTypeHandler();
    }

    @Override
    public void setActiveFileTypeHandler(IHopFileTypeHandler activeFileTypeHandler) {

    }

    @Override
    public List<IHopFileType> getSupportedHopFileTypes() {
        return Collections.emptyList();
    }

    @GuiKeyboardShortcut(control = true, shift = true, key = 'c')
    @GuiOsxKeyboardShortcut(command = true, shift = true, key = 'c')
    @Override
    public void activate() {
        hopGui.setActivePerspective(this);
    }

    @Override
    public void perspectiveActivated() {

    }

    @Override
    public void navigateToPreviousFile() {

    }

    @Override
    public void navigateToNextFile() {

    }

    @Override
    public boolean isActive() {
        return hopGui.isActivePerspective(this);
    }

    @Override
    public void initialize(HopGui hopGui, Composite parent) {
        this.hopGui = hopGui;
        PropsUi props = PropsUi.getInstance();

        composite = new Composite(parent, SWT.NONE);
        FormLayout clayout = new FormLayout();
        clayout.marginLeft = props.getMargin();
        clayout.marginTop = props.getMargin();
        clayout.marginLeft = props.getMargin();
        clayout.marginBottom = props.getMargin();
        composite.setLayout(clayout);
        props.setLook(composite);

        FormData cformData = new FormData();
        cformData.left = new FormAttachment(0,0);
        cformData.top = new FormAttachment(0,0);
        cformData.right = new FormAttachment(100,-3);
        cformData.bottom = new FormAttachment(100,-5);
        composite.setLayoutData(cformData);

        configTabs = new CTabFolder(composite, SWT.BORDER);
        props.setLook(configTabs, Props.WIDGET_STYLE_TAB);

        FormLayout layout = new FormLayout();
        layout.marginLeft = props.getMargin();
        layout.marginTop = props.getMargin();
        layout.marginLeft = props.getMargin();
        layout.marginBottom = props.getMargin();
        configTabs.setLayout(layout);
        props.setLook(configTabs);

        FormData formData = new FormData();
        formData.left = new FormAttachment(00,0);
        formData.top = new FormAttachment(0,00);
        formData.right = new FormAttachment(100,-3);
        formData.bottom = new FormAttachment(100,-5);
        configTabs.setLayoutData(formData);
        configTabs.setMaximized(true);

        Label label = new Label(configTabs, SWT.LEFT);
        label.setText("dummy text");
        FormData fdlFields = new FormData();
        fdlFields.left = new FormAttachment(0, 0);
        fdlFields.top = new FormAttachment(0, props.getMargin());
        label.setLayoutData(fdlFields);
        props.setLook(label);

        Label wlInfo = new Label(configTabs, SWT.LEFT);
        props.setLook(wlInfo);
        wlInfo.setText(BaseMessages.getString(PKG, "HopSearchPerspective.Header.Description.Text"));
        wlInfo.setFont(GuiResource.getInstance().getFontBold());
        FormData fdInfo = new FormData();
        fdInfo.left = new FormAttachment(0, 0);
        fdInfo.right = new FormAttachment(100, 0);
        fdInfo.top = new FormAttachment(0, 0);
        wlInfo.setLayoutData(fdInfo);
        Control lastControl = wlInfo;

        Label wlSep1 = new Label(configTabs, SWT.SEPARATOR | SWT.HORIZONTAL);
        props.setLook(wlSep1);
        FormData fdlSep1 = new FormData();
        fdlSep1.left = new FormAttachment(0, 0);
        fdlSep1.right = new FormAttachment(100, 0);
        fdlSep1.top = new FormAttachment(lastControl, 0);
        wlSep1.setLayoutData(fdlSep1);

        GuiRegistry guiRegistry = GuiRegistry.getInstance();
        List<GuiTabItem> tabsList = guiRegistry.getGuiTabsMap().get(CONFIG_PERSPECTIVE_TABS);

        if(tabsList != null){
            tabsList.sort(Comparator.comparing(GuiTabItem::getId));
            for(GuiTabItem tabItem : tabsList){
                try {
                    Object object = tabItem.getMethod().getDeclaringClass().getConstructor().newInstance();
                    tabItem.getMethod().invoke(object, configTabs);
                }catch(Exception e){
                    throw new RuntimeException(
                        "unable to invoke tab method with the parent composite as argument", e
                    );
                }
            }
        }
    }

    @Override
    public boolean hasNavigationPreviousFile() {
        return false;
    }

    @Override
    public boolean hasNavigationNextFile() {
        return false;
    }

    @Override
    public Control getControl() {
        return composite;
    }

    @Override
    public boolean remove(IHopFileTypeHandler typeHandler) {
        return false;
    }

    @Override
    public List<TabItemHandler> getItems() {
        return null;
    }

    @Override
    public List<ISearchable> getSearchables() {
        List<ISearchable> searchables = new ArrayList<>();
        return searchables;
    }


    @Override
    public void closeTab(CTabFolderEvent event, CTabItem tabItem) {

    }

    @Override
    public List<CTabItem> getTabsToRight(CTabItem selectedTabItem) {
        return TabClosable.super.getTabsToRight(selectedTabItem);
    }

    @Override
    public List<CTabItem> getTabsToLeft(CTabItem selectedTabItem) {
        return TabClosable.super.getTabsToLeft(selectedTabItem);
    }

    @Override
    public List<CTabItem> getOtherTabs(CTabItem selectedTabItem) {
        return TabClosable.super.getOtherTabs(selectedTabItem);
    }

    @Override
    public CTabFolder getTabFolder() {
        return configTabs;
    }
}
