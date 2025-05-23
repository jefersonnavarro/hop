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

package org.apache.hop.ui.hopgui;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.apache.hop.core.Const;
import org.apache.hop.core.HopClientEnvironment;
import org.apache.hop.core.action.GuiContextAction;
import org.apache.hop.core.action.GuiContextActionFilter;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopPluginException;
import org.apache.hop.core.exception.HopXmlException;
import org.apache.hop.core.gui.plugin.GuiElementType;
import org.apache.hop.core.gui.plugin.GuiPluginType;
import org.apache.hop.core.gui.plugin.GuiRegistry;
import org.apache.hop.core.gui.plugin.GuiWidgetElement;
import org.apache.hop.core.gui.plugin.callback.GuiCallback;
import org.apache.hop.core.gui.plugin.key.GuiKeyboardShortcut;
import org.apache.hop.core.gui.plugin.key.GuiOsxKeyboardShortcut;
import org.apache.hop.core.gui.plugin.menu.GuiMenuElement;
import org.apache.hop.core.gui.plugin.tab.GuiTab;
import org.apache.hop.core.gui.plugin.toolbar.GuiToolbarElement;
import org.apache.hop.core.gui.plugin.toolbar.GuiToolbarElementFilter;
import org.apache.hop.core.plugins.IPlugin;
import org.apache.hop.core.plugins.IPluginType;
import org.apache.hop.core.plugins.PluginRegistry;
import org.apache.hop.core.search.SearchableAnalyserPluginType;
import org.apache.hop.core.xml.XmlHandler;
import org.apache.hop.ui.hopgui.file.HopFileTypePluginType;
import org.apache.hop.ui.hopgui.file.HopFileTypeRegistry;
import org.apache.hop.ui.hopgui.file.IHopFileType;
import org.apache.hop.ui.hopgui.perspective.HopPerspectivePluginType;
import org.apache.hop.ui.util.EnvironmentUtils;
import org.eclipse.swt.SWT;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class HopGuiEnvironment extends HopClientEnvironment {

  public static void init() throws HopException {
    init(
        List.of(
            GuiPluginType.getInstance(),
            HopPerspectivePluginType.getInstance(),
            HopFileTypePluginType.getInstance(),
            SearchableAnalyserPluginType.getInstance()));
  }

  public static void init(List<IPluginType> pluginTypes) throws HopException {
    pluginTypes.forEach(PluginRegistry::addPluginType);

    for (IPluginType pluginType : pluginTypes) {
      pluginType.searchPlugins();
    }

    initGuiPlugins();
  }

  /**
   * Look for GuiWidgetElement annotated fields in all the GuiPlugins. Put them in the Gui registry
   *
   * @throws HopException
   */
  public static void initGuiPlugins() throws HopException {
    List<String> excludedGuiElements = new ArrayList<>();

    // Try loading code exclusions
    try {
      String path = Const.HOP_CONFIG_FOLDER + File.separator + "disabledGuiElements.xml";

      Document document = XmlHandler.loadXmlFile(path);
      Node exclusionsNode = XmlHandler.getSubNode(document, "exclusions");
      List<Node> exclusionNodes = XmlHandler.getNodes(exclusionsNode, "exclusion");

      for (Node exclusionNode : exclusionNodes) {
        excludedGuiElements.add(exclusionNode.getTextContent());
      }
    } catch (HopXmlException e) {
      // ignore
    }

    try {
      GuiRegistry guiRegistry = GuiRegistry.getInstance();
      PluginRegistry pluginRegistry = PluginRegistry.getInstance();

      List<IPlugin> guiPlugins = pluginRegistry.getPlugins(GuiPluginType.class);
      for (IPlugin guiPlugin : guiPlugins) {
        ClassLoader classLoader = pluginRegistry.getClassLoader(guiPlugin);
        Class<?>[] typeClasses = guiPlugin.getClassMap().keySet().toArray(new Class<?>[0]);
        String guiPluginClassName = guiPlugin.getClassMap().get(typeClasses[0]);
        Class<?> guiPluginClass = classLoader.loadClass(guiPluginClassName);

        // Component widgets are defined on fields
        //
        List<Field> fields = findDeclaredFields(guiPluginClass);

        for (Field field : fields) {
          GuiWidgetElement guiElement = field.getAnnotation(GuiWidgetElement.class);
          if (guiElement != null && !excludedGuiElements.contains(guiElement.id())) {
            // Add the GUI Element to the registry...
            //
            guiRegistry.addGuiWidgetElement(guiPluginClassName, guiElement, field);
          }
        }

        // Menu and toolbar items are defined on methods
        //
        List<Method> methods = findDeclaredMethods(guiPluginClass);
        for (Method method : methods) {
          GuiMenuElement menuElement = method.getAnnotation(GuiMenuElement.class);
          if (menuElement != null) {
            guiRegistry.addGuiMenuElement(guiPluginClassName, menuElement, method, classLoader);
          }
          GuiToolbarElement toolbarElement = method.getAnnotation(GuiToolbarElement.class);
          if (toolbarElement != null && !excludedGuiElements.contains(toolbarElement.id())) {
            guiRegistry.addGuiToolbarElement(
                guiPluginClassName, toolbarElement, method, classLoader);
          }
          GuiToolbarElementFilter toolbarElementFilter =
              method.getAnnotation(GuiToolbarElementFilter.class);
          if (toolbarElementFilter != null) {
            guiRegistry.addGuiToolbarItemFilter(
                guiPluginClassName, method, toolbarElementFilter, classLoader);
          }
          GuiKeyboardShortcut shortcut = method.getAnnotation(GuiKeyboardShortcut.class);
          if (shortcut != null) {
            // RAP does not support ESC as a shortcut key.
            if (EnvironmentUtils.getInstance().isWeb() && shortcut.key() == SWT.ESC) {
              continue;
            }
            guiRegistry.addKeyboardShortcut(guiPluginClassName, method, shortcut);
          }
          GuiOsxKeyboardShortcut osxShortcut = method.getAnnotation(GuiOsxKeyboardShortcut.class);
          if (osxShortcut != null) {
            // RAP does not support ESC as a shortcut key.
            if (EnvironmentUtils.getInstance().isWeb() && osxShortcut.key() == SWT.ESC) {
              continue;
            }
            guiRegistry.addKeyboardShortcut(guiPluginClassName, method, osxShortcut);
          }
          GuiContextAction contextAction = method.getAnnotation(GuiContextAction.class);
          if (contextAction != null && !excludedGuiElements.contains(contextAction.id())) {
            guiRegistry.addGuiContextAction(guiPluginClassName, method, contextAction, classLoader);
          }

          GuiContextActionFilter actionFilter = method.getAnnotation(GuiContextActionFilter.class);
          if (actionFilter != null) {
            guiRegistry.addGuiActionFilter(guiPluginClassName, method, actionFilter, classLoader);
          }

          GuiCallback guiCallback = method.getAnnotation(GuiCallback.class);
          if (guiCallback != null) {
            guiRegistry.registerGuiCallback(guiPluginClass, method, guiCallback);
          }

          GuiWidgetElement guiWidgetElement = method.getAnnotation(GuiWidgetElement.class);
          if (guiWidgetElement != null && !excludedGuiElements.contains(guiWidgetElement.id())) {
            if (guiWidgetElement.type() == GuiElementType.COMPOSITE) {
              guiRegistry.addCompositeGuiWidgetElement(guiWidgetElement, method, classLoader);
            } else {
              guiRegistry.addGuiWidgetElement(
                  guiWidgetElement, method, guiPluginClassName, classLoader);
            }
          }

          GuiTab guiTab = method.getAnnotation(GuiTab.class);
          if (guiTab != null && !excludedGuiElements.contains(guiTab.id())) {
            guiRegistry.addGuiTab(guiPluginClassName, method, guiTab, classLoader);
          }
        }
      }

      // Sort all GUI elements once.
      //
      guiRegistry.sortAllElements();

      // Now populate the HopFileTypeRegistry
      //
      // Get all the file handler plugins
      //
      PluginRegistry registry = PluginRegistry.getInstance();
      List<IPlugin> plugins = registry.getPlugins(HopFileTypePluginType.class);
      for (IPlugin plugin : plugins) {
        try {
          IHopFileType hopFileTypeInterface = registry.loadClass(plugin, IHopFileType.class);
          HopFileTypeRegistry.getInstance().registerHopFile(hopFileTypeInterface);
        } catch (HopPluginException e) {
          throw new HopException(
              "Unable to load plugin with ID '"
                  + plugin.getIds()[0]
                  + "' and type : "
                  + plugin.getPluginType().getName(),
              e);
        }
      }
    } catch (Exception e) {
      throw new HopException("Error looking for Elements in GUI Plugins ", e);
    }
  }
}
