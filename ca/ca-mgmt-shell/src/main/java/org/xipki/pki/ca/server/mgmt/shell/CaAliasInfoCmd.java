/*
 *
 * This file is part of the XiPKI project.
 * Copyright (c) 2013 - 2016 Lijun Liao
 * Author: Lijun Liao
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation with the addition of the
 * following permission added to Section 15 as permitted in Section 7(a):
 * FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
 * THE AUTHOR LIJUN LIAO. LIJUN LIAO DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
 * OF THIRD PARTY RIGHTS.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the XiPKI software without
 * disclosing the source code of your own applications.
 *
 * For more information, please contact Lijun Liao at this
 * address: lijun.liao@gmail.com
 */

package org.xipki.pki.ca.server.mgmt.shell;

import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.xipki.pki.ca.server.mgmt.shell.completer.CaAliasCompleter;

/**
 * @author Lijun Liao
 */

@Command(scope = "xipki-ca", name = "caalias-info",
        description = "show information of CA alias")
@Service
public class CaAliasInfoCmd extends CaCommandSupport {

    @Argument(index = 0, name = "alias", description = "CA alias")
    @Completion(CaAliasCompleter.class)
    private String caAlias;

    @Override
    protected Object doExecute()
    throws Exception {
        Set<String> aliasNames = caManager.getCaAliasNames();

        StringBuilder sb = new StringBuilder();

        if (caAlias == null) {
            int n = aliasNames.size();

            if (n == 0 || n == 1) {
                sb.append((n == 0)
                        ? "no"
                        : "1");
                sb.append(" CA alias is configured\n");
            } else {
                sb.append(n).append(" CA aliases are configured:\n");
            }

            List<String> sorted = new ArrayList<>(aliasNames);
            Collections.sort(sorted);

            for (String aliasName : sorted) {
                sb.append("\t").append(aliasName).append("\n");
            }
        } else {
            if (aliasNames.contains(caAlias)) {
                String paramValue = caManager.getCaNameForAlias(caAlias);
                sb.append(caAlias).append("\n\t").append(paramValue);
            } else {
                throw new UnexpectedException("could not find CA alias '" + caAlias + "'");
            }
        }

        out(sb.toString());
        return null;
    } // method doExecute

}
