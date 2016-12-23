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

package com.github.dnvriend

import scala.util.Try

class RegisterFacade(personRepository: PersonRepository, addressRepository: AddressRepository) {
  def register(name: String, age: Int, street: String, housenr: Int, zipcode: String): Try[String] = for {
    _ <- Try(personRepository.save(name, age))
    _ <- Try(addressRepository.save(street, housenr, zipcode))
  } yield "registered"
}
