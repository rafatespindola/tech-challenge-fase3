package br.com.fiap.agendamento.service;

import br.com.fiap.agendamento.dto.request.CriarProfissionalRequest;
import br.com.fiap.agendamento.dto.response.CriarProfissionalResponse;
import br.com.fiap.agendamento.entity.Profissional;
import br.com.fiap.agendamento.repository.ProfissionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfissionalService {

    private final ProfissionalRepository profissionalRepository;

    public CriarProfissionalResponse criarProfissional(CriarProfissionalRequest request) {

        Profissional profissional = Profissional.builder()
                .nome(request.nome())
                .build();

        Profissional profissionalSalvo = profissionalRepository.save(profissional);
        return converteParaResponse(profissionalSalvo);
    }

    public CriarProfissionalResponse converteParaResponse(Profissional profissional){
        return new CriarProfissionalResponse(profissional.getId(), profissional.getNome());
    }
}
