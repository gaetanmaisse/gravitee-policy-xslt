/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.policy.xslt.transformer.saxon;

import javax.xml.XMLConstants;
import javax.xml.transform.TransformerConfigurationException;
import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.lib.FeatureKeys;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */

public class SaxonTransformerFactory extends TransformerFactoryImpl {

    public SaxonTransformerFactory() {
        super();
        enableSecureProcessing();
    }

    private void enableSecureProcessing() {
        try {
            this.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            this.setAttribute(FeatureKeys.ALLOWED_PROTOCOLS, "");
            this.setAttribute(FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS, false);
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException("Failed to enable SaxonTransformer security", e);
        }
    }
}
