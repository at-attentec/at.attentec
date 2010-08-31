/***
 * Copyright (c) 2010 Attentec AB, http://www.attentec.se
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
package com.attentec.test;

import com.attentec.Attentec;

import android.test.ActivityInstrumentationTestCase2;

/**
 * Test for Attentec.java.
 * @author David Granqvist
 *
 */
public class AttentecTest extends ActivityInstrumentationTestCase2<Attentec> {

	/**
	 * Instance of Attentec activity.
	 */
	private Attentec attentec;

	/**
	 * Sets up test environment for testing Attentec class.
	 */
	public AttentecTest() {
		super("com.attentec", Attentec.class);
	}

	@Override
	protected final void setUp() throws Exception {
		super.setUp();
		attentec = new Attentec();
	}

	/**
	 * Simple test to test if tests in general work and
	 * that creating an instance of Attentec does not fail.
	 */
	public final void testPreConditions() {
		assertTrue(null == null); //This better be right!
		assertTrue(attentec != null);
	}

}
