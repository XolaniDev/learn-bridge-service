package co.za.learn.bridge.model.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;
    @NotBlank
    @Size(max = 50)
    private String name;
    @NotBlank
    @Size(max = 50)
    private String surname;
    @NotBlank
    @Size(max = 50)
    @Email
    private String email;
    @NotBlank
    @Size(max = 15)
    private String phoneNumber;
    @NotBlank
    @Size(max = 120)
    private String password;
    private String grade;
    private List<String> interests;
    private List<String> subjects;
    private String financialBackground;
    private String province;

    @DBRef
    private Set<Role> roles = new HashSet<>();
    @NotBlank
    private Date createdDate;

    public User() {
    }

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

}
