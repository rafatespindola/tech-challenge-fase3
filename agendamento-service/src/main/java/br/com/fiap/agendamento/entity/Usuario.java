package br.com.fiap.agendamento.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

@Getter
@Entity
@Table(name = "usuario",
        uniqueConstraints = @UniqueConstraint(name = "uk_usuario_login", columnNames = "login"))
@Check(name = "ck_usuario_vinculo", constraints = """
        (role = 'PACIENTE' AND paciente_id IS NOT NULL AND profissional_id IS NULL)
        OR (role IN ('MEDICO','ENFERMEIRO') AND profissional_id IS NOT NULL AND paciente_id IS NULL)
        """)
public class Usuario extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String login;

    /** Hash BCrypt. Nunca exposto em nenhum schema GraphQL. */
    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "paciente_id", foreignKey = @ForeignKey(name = "fk_usuario_paciente"))
    private Paciente paciente;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "profissional_id", foreignKey = @ForeignKey(name = "fk_usuario_profissional"))
    private Profissional profissional;

    protected Usuario() {
    }

    /** Usuario de paciente. */
    public static Usuario paraPaciente(String login, String senhaHash, Paciente paciente) {
        Usuario u = new Usuario();
        u.login = login;
        u.senha = senhaHash;
        u.role = Role.PACIENTE;
        u.paciente = paciente;
        return u;
    }

    /** Usuario de profissional. A role vem do cargo, para nao divergirem. */
    public static Usuario paraProfissional(String login, String senhaHash, Profissional profissional) {
        Usuario u = new Usuario();
        u.login = login;
        u.senha = senhaHash;
        u.role = profissional.getCargo() == Cargo.MEDICO ? Role.MEDICO : Role.ENFERMEIRO;
        u.profissional = profissional;
        return u;
    }

    /** Nome de exibicao: vem da pessoa vinculada, nunca duplicado aqui. */
    public String getNome() {
        return paciente != null ? paciente.getNome() : profissional.getNome();
    }

    public Long getPacienteId() {
        return paciente != null ? paciente.getId() : null;
    }

    public Long getProfissionalId() {
        return profissional != null ? profissional.getId() : null;
    }

}