package co.za.learn.bridge.model.dto;

import lombok.Getter;

@Getter
public enum ERole {
  ROLE_USER("General User"),
  ROLE_ADMIN("Administrator");

  private final String value;

  ERole(String value) {
    this.value = value;
  }

  public static ERole fromValue(String value) {
    for (ERole role : ERole.values()) {
      if (role.value.equalsIgnoreCase(value)) {
        return role;
      }
    }
    throw new IllegalArgumentException("Invalid role value: " + value);
  }

}
