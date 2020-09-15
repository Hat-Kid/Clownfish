/*
 * Copyright 2019 sulzbachr.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.clownfish.clownfish.daointerface;

import io.clownfish.clownfish.dbentities.CfTemplateversion;
import java.util.List;

/**
 *
 * @author sulzbachr
 */
public interface CfTemplateversionDAO {
    List<CfTemplateversion> findByTemplateref(long ref);
    long findMaxVersion(long ref);
    CfTemplateversion findByPK(long ref, long version);
    List<CfTemplateversion> findAll();
    CfTemplateversion create(CfTemplateversion entity);
    boolean delete(CfTemplateversion entity);
    CfTemplateversion edit(CfTemplateversion entity);
}
