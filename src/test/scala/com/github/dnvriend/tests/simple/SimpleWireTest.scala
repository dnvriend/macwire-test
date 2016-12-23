/*
 * Copyright 2016 Dennis Vriend
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
 */

package com.github.dnvriend.tests.simple

import com.github.dnvriend._
import com.softwaremill.macwire._

class SimpleWireTest extends TestSpec {
  it should "wire the facade" in {
    lazy val dbApi = wire[DBApi]
    lazy val addressRepo = wire[AddressRepository]
    lazy val personRepo = wire[PersonRepository]
    lazy val regFacade = wire[RegisterFacade]
    regFacade.register("foo", 42, "bar", 42, "baz").success.value shouldBe "registered"
  }
}
