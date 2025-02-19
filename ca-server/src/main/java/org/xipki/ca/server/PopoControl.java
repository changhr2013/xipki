/*
 *
 * Copyright (c) 2013 - 2020 Lijun Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xipki.ca.server;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.jcajce.interfaces.XDHKey;
import org.bouncycastle.util.encoders.Base64;
import org.xipki.security.AlgorithmValidator;
import org.xipki.security.CollectionAlgorithmValidator;
import org.xipki.security.DHSigStaticKeyCertPair;
import org.xipki.security.X509Cert;
import org.xipki.security.util.KeyUtil;
import org.xipki.util.CollectionUtil;
import org.xipki.util.ConfPairs;
import org.xipki.util.InvalidConfException;
import org.xipki.util.StringUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Popo (proof-of-possession) control.
 *
 * @author Lijun Liao
 */
public class PopoControl {

  public static final String KEY_SIGALGO = "sigalgo";

  public static final String KEY_DH_TYPE = "dh.type";

  public static final String KEY_DH_PASSWORD = "dh.password";

  public static final String KEY_DH_KEYSTORE = "dh.keystore";

  private final String conf;

  private final CollectionAlgorithmValidator popoAlgoValidator;

  private final List<DHSigStaticKeyCertPair> dhKeyAndCerts = new ArrayList<>(1);

  private final X509Cert[] dhCerts;

  public PopoControl(String conf)
      throws InvalidConfException {
    this.conf = conf;

    ConfPairs pairs = new ConfPairs(conf);

    // popo signature algorithms
    String name = KEY_SIGALGO;
    String str = pairs.value(name);
    if (str == null) {
      this.popoAlgoValidator = CollectionAlgorithmValidator.INSTANCE;
    } else {
      try {
        Set<String> algos = StringUtil.splitAsSet(str, ":");
        this.popoAlgoValidator = CollectionAlgorithmValidator.buildAlgorithmValidator(algos);
      } catch (NoSuchAlgorithmException ex) {
        throw new InvalidConfException("invalid " + name + ": " + str, ex);
      }
    }

    // Diffie-Hellman based Popo
    String type = pairs.value(KEY_DH_TYPE);
    String passwordStr = pairs.value(KEY_DH_PASSWORD);
    String keystoreStr = pairs.value(KEY_DH_KEYSTORE);
    if (StringUtil.isBlank(type) && StringUtil.isBlank(passwordStr)
        && StringUtil.isBlank(keystoreStr)) {
      dhCerts = null;
    } else {
      if (StringUtil.isBlank(type)) {
        throw new InvalidConfException(KEY_DH_TYPE + " is not defined in conf");
      }

      if (StringUtil.isBlank(keystoreStr)) {
        throw new InvalidConfException(KEY_DH_KEYSTORE + " is not defined in conf");
      }

      if (StringUtil.isBlank(passwordStr)) {
        throw new InvalidConfException(KEY_DH_PASSWORD + " is not defined in conf");
      }

      InputStream is;
      if (keystoreStr.startsWith("base64:")) {
        byte[] bytes = Base64.decode(keystoreStr.substring("base64:".length()));
        is = new ByteArrayInputStream(bytes);
      } else {
        throw new InvalidConfException("keystore not start with 'base64:'");
      }

      try {
        char[] password = passwordStr.toCharArray();
        KeyStore ks = KeyUtil.getKeyStore(type);
        ks.load(is, password);

        Enumeration<String> aliases = ks.aliases();
        List<X509Cert> certs = new LinkedList<>();
        while (aliases.hasMoreElements()) {
          String alias = aliases.nextElement();
          if (!ks.isKeyEntry(alias)) {
            continue;
          }

          PrivateKey key = (PrivateKey) ks.getKey(alias, password);
          if (!(key instanceof XDHKey)) {
            // we consider only XDH key
            continue;
          }
          X509Cert cert = new X509Cert((X509Certificate) ks.getCertificate(alias));

          dhKeyAndCerts.add(new DHSigStaticKeyCertPair(key, cert));
          certs.add(cert);
        }

        this.dhCerts = certs.toArray(new X509Cert[0]);
      } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException
          | UnrecoverableKeyException | ClassCastException ex) {
        throw new InvalidConfException("invalid dhStatic popo configuration", ex);
      }
    }
  } // constructor

  public X509Cert[] getDhCertificates() {
    if (dhCerts == null || dhCerts.length == 0) {
      return null;
    } else {
      return Arrays.copyOf(dhCerts, dhCerts.length);
    }
  } // method getCertificates

  public DHSigStaticKeyCertPair getDhKeyCertPair(X500Name issuer, BigInteger serial,
      String keyAlgorithm) {
    if (dhKeyAndCerts == null) {
      return null;
    }

    for (DHSigStaticKeyCertPair m : dhKeyAndCerts) {
      if (m.getIssuer().equals(issuer)
          && m.getSerialNumber().equals(serial)
          && m.getPrivateKey().getAlgorithm().equalsIgnoreCase(keyAlgorithm)) {
        return m;
      }
    }

    return null;
  } // method getKeyCertPair

  public String getConf() {
    return conf;
  }

  public AlgorithmValidator getPopoAlgoValidator() {
    return popoAlgoValidator;
  }

  public String toString(boolean verbose) {
    // TODO: remove sensitive data
    return StringUtil.concatObjects(
        "signature algorithms: ", CollectionUtil.sort(popoAlgoValidator.getAlgoNames()),
        (verbose ? "\n  encoded: " : ""), (verbose ? conf : ""));
  } // method toString

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof PopoControl)) {
      return false;
    }

    return conf.equals(((PopoControl) obj).conf);
  }

}
