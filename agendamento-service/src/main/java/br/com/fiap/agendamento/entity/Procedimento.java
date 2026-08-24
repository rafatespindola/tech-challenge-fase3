package br.com.fiap.agendamento.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "procedimento",
        uniqueConstraints = @UniqueConstraint(name = "uk_procedimento_nome", columnNames = "nome"))
public class Procedimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "duracao_minutos", nullable = false)
    private Integer duracaoMinutos;

    protected Procedimento() {
    }

    public Procedimento(String nome, Integer duracaoMinutos) {
        this.nome = nome;
        this.duracaoMinutos = duracaoMinutos;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public Integer getDuracaoMinutos() { return duracaoMinutos; }
}
