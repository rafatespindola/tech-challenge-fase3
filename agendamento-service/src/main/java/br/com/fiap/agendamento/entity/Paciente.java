package br.com.fiap.agendamento.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;


@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "paciente",
        uniqueConstraints = @UniqueConstraint(name = "uk_paciente_cpf", columnNames = "cpf"))
public class Paciente extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, length = 13)
    private String telefone;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    private String email;

    @Column(length = 11)
    private String cpf;

}
