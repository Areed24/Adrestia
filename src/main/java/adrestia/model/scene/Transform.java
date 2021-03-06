/*
Apache2 License Notice
Copyright 2017 Alex Barry

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package adrestia;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
* Represents a Transformation between coordinate systems.
*/
public class Transform {

  private double[] translation;
  private double[] rotation;

  /**
  * Default empty Transform constructor.
  */
  public Transform() {
    super();
    this.translation = new double[3];
    this.rotation = new double[4];
  }

  /**
  * Default Transform constructor.
  * @param newTranslation A Double Array with 3 values (x, y, z).
  * @param newRotation A Double Array with 4 values (theta, x, y, z).
  */
  public Transform(double[] newTranslation, double[] newRotation) {
    super();
    this.translation = newTranslation;
    this.rotation = newRotation;
  }

  /**
  * Returns value of translation.
  * @return A Double Array with 3 values (x, y, z).
  */
  @JsonGetter("translation")
  public double[] getTranslation() {
    return this.translation;
  }

  /**
  * Returns value of rotation.
  * @return A Double Array with 4 values (theta, x, y, z).
  */
  @JsonGetter("rotation")
  public double[] getRotation() {
    return this.rotation;
  }

  /**
  * Sets new value of translation.
  * @param newTranslation A Double Array with 3 values (x, y, z).
  */
  @JsonSetter("translation")
  public void setTranslation(double[] newTranslation) {
    this.translation = newTranslation;
  }

  /**
  * Sets new value of rotation.
  * @param newRotation A Double Array with 4 values (theta, x, y, z).
  */
  @JsonSetter("rotation")
  public void setRotation(double[] newRotation) {
    this.rotation = newRotation;
  }
}
