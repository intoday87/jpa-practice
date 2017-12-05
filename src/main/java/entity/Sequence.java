package entity;

import lombok.Setter;

import javax.persistence.*;

@Entity
@SequenceGenerator(
    name = "SEQ_GENERATOR",
    sequenceName = "SEQ",
    initialValue = 1,
    allocationSize = 1
)
public class Sequence {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GENERATOR")
    private Long id;
}
