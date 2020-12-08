/*! ******************************************************************************
 *
 * Hop : The Hop Orchestration Platform
 *
 * http://www.project-hop.org
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.apache.hop.git.xp;

import org.apache.commons.lang.StringUtils;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.extension.ExtensionPoint;
import org.apache.hop.core.extension.IExtensionPoint;
import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.git.HopGitPerspective;
import org.apache.hop.ui.hopgui.shared.AuditManagerGuiUtil;

/** Reload the available git repositories when an environment is changed... */
@ExtensionPoint(
    id = "HopGuiEnvironmentActivated",
    extensionPointId = "EnvironmentActivated",
    description = "Reload the available git repositories when an environment is changed")
public class HopGuiEnvironmentActivated implements IExtensionPoint<String> {

  @Override
  public void callExtensionPoint(ILogChannel log, String environmentName) throws HopException {
    HopGitPerspective gitPerspective = HopGitPerspective.getInstance();
    gitPerspective.refreshGitRepositoriesList();
    gitPerspective.clearRepository();

    // See if there's a history.  Open the last used git repository
    //
    String lastUsedRepositoryName =
        AuditManagerGuiUtil.getLastUsedValue(HopGitPerspective.AUDIT_TYPE);
    if (StringUtils.isEmpty(lastUsedRepositoryName)) {
      return;
    }

    gitPerspective.loadRepository(lastUsedRepositoryName);
  }
}
