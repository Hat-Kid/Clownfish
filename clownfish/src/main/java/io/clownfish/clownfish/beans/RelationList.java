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
package io.clownfish.clownfish.beans;

import io.clownfish.clownfish.dbentities.CfClass;
import io.clownfish.clownfish.serviceinterface.CfClassService;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.event.SelectEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author sulzbachr
 */
@Named("relationList")
@Scope("session")
@Component
public class RelationList {
    @Autowired transient CfClassService cfclassService;
    
    private @Getter @Setter List<CfClass> classListe;
    private @Getter @Setter CfClass selectedClass1 = null;
    private @Getter @Setter CfClass selectedClass2 = null;
    
    @PostConstruct
    public void init() {
        classListe = cfclassService.findAll();
    }
    
}