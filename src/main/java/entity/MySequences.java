package entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Getter
@Setter
@Entity(name = "MY_SEQUENCES")
public class MySequences {

    @Id
    @Column(name = "sequence_name")
    private String sequenceName;

    @Column(name = "next_val")
    private Long nextVal;
}
