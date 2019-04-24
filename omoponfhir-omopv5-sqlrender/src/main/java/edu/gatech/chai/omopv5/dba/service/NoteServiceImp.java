/*******************************************************************************
 * Copyright (c) 2019 Georgia Tech Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package edu.gatech.chai.omopv5.dba.service;

import org.springframework.stereotype.Service;

import edu.gatech.chai.omopv5.model.entity.Note;
import edu.gatech.chai.omopv5.sqlrender.dao.NoteDao;

@Service
public class NoteServiceImp extends BaseEntityServiceImp<Note, NoteDao> implements NoteService {

	public NoteServiceImp() {
		super(Note.class);
	}

}
