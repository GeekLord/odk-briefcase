/*
 * Copyright (C) 2026 ODK Briefcase contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.briefcase.util;

import java.util.concurrent.locks.ReentrantLock;
import org.javarosa.core.model.FormDef;
import org.javarosa.xform.parse.XFormParser;

/**
 * Serializes all XForm parsing done through JavaRosa.
 * <p>
 * JavaRosa's {@link XFormParser} only allows one form to be parsed at a time per
 * JVM and, instead of waiting, fails fast when another parse is in progress.
 * Briefcase parses forms from several threads (the form cache on the UI thread,
 * parallel pull, push and export jobs), so every call to {@link XFormParser#parse()}
 * must go through {@link #parse(XFormParser)} to wait for its turn instead of failing.
 */
public final class XFormParsing {
  private static final ReentrantLock LOCK = new ReentrantLock(true);

  private XFormParsing() {
  }

  public static FormDef parse(XFormParser parser) throws XFormParser.ParseException {
    LOCK.lock();
    try {
      return parser.parse();
    } finally {
      LOCK.unlock();
    }
  }
}
