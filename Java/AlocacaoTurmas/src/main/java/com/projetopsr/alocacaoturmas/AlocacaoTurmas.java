package com.projetopsr.alocacaoturmas;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class AlocacaoTurmas {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Erro: Forneça a instância (facil, media, dificil) e a heurística (MRV, DOM_W_DEG).");
            return;
        }
        String nomeInstancia = args[0].toLowerCase();
        String nomeHeuristica = args[1].toUpperCase();

        // --- Dados de Entrada ---
        int nTurmas = 0, nSalas = 0, nHorarios = 0;
        boolean isOptimizationProblem = false;
        int[] alunosTurma = {}, capacidadeSala = {}, professorTurma = {};
        int turmaA = 0, turmaC = 0, salaLab1 = 0, horarioH1 = 0;

        if ("facil".equals(nomeInstancia)) {
            nTurmas = 2; nSalas = 2; nHorarios = 2; isOptimizationProblem = false;
            alunosTurma = new int[]{20, 25}; capacidadeSala = new int[]{25, 30}; professorTurma = new int[]{1, 2};
            turmaA = 1; turmaC = 99; salaLab1 = 99; horarioH1 = 1;
        } else if ("media".equals(nomeInstancia)) {
            nTurmas = 3; nSalas = 3; nHorarios = 3; isOptimizationProblem = false;
            alunosTurma = new int[]{20, 25, 30}; capacidadeSala = new int[]{25, 30, 35}; professorTurma = new int[]{1, 1, 2};
            turmaA = 1; turmaC = 3; salaLab1 = 3; horarioH1 = 1;
        } else if ("dificil".equals(nomeInstancia)) {
            nTurmas = 3; nSalas = 3; nHorarios = 3; isOptimizationProblem = true;
            alunosTurma = new int[]{23, 28, 33}; capacidadeSala = new int[]{25, 30, 35}; professorTurma = new int[]{1, 1, 2};
            turmaA = 1; turmaC = 3; salaLab1 = 3; horarioH1 = 1;
        } else {
            System.out.println("Erro: Instância '" + nomeInstancia + "' não reconhecida.");
            return;
        }

        Model model = new Model("Alocacao de Turmas");
        IntVar[] salaTurma = model.intVarArray("sala", nTurmas, 1, nSalas);
        IntVar[] horarioTurma = model.intVarArray("horario", nTurmas, 1, nHorarios);
        BoolVar c4Violada = model.boolVar("c4_violada");

        // --- Restrições ---
        // Regra de Capacidade (Versão Corrigida e Explícita)
        for (int t = 0; t < nTurmas; t++) {
            for (int s = 0; s < nSalas; s++) {
                // Se a sala da turma 't' for a sala 's+1'...
                model.ifThen(
                    model.arithm(salaTurma[t], "=", s + 1),
                    // ...então a capacidade dessa sala deve ser suficiente.
                    model.arithm(model.intVar(capacidadeSala[s]), ">=", alunosTurma[t])
                );
            }
        }

        // Outras restrições (permanecem iguais)
        for (int t1 = 0; t1 < nTurmas; t1++) {
            for (int t2 = t1 + 1; t2 < nTurmas; t2++) {
                model.ifThen(model.arithm(horarioTurma[t1], "=", horarioTurma[t2]), model.arithm(salaTurma[t1], "!=", salaTurma[t2]));
                if (professorTurma[t1] == professorTurma[t2]) {
                    model.arithm(horarioTurma[t1], "!=", horarioTurma[t2]).post();
                }
            }
        }

        if (nTurmas >= turmaC) {
            model.ifThen(model.arithm(horarioTurma[turmaA - 1], "=", horarioH1), model.arithm(salaTurma[turmaA - 1], "!=", salaTurma[turmaC - 1]));
        }
        
        if (nTurmas >= turmaC && isOptimizationProblem) {
            model.ifOnlyIf(model.arithm(c4Violada, "=", 1), model.arithm(salaTurma[turmaC - 1], "=", salaLab1));
        } else {
            if (nTurmas >= turmaC && salaLab1 <= nSalas) {
                model.arithm(salaTurma[turmaC - 1], "!=", salaLab1).post();
            }
            model.arithm(c4Violada, "=", 0).post();
        }

        if (isOptimizationProblem) {
            model.setObjective(Model.MINIMIZE, c4Violada);
        }
        
        Solver solver = model.getSolver();
        IntVar[] todasAsVariaveis = Arrays.copyOf(salaTurma, salaTurma.length + horarioTurma.length);
        System.arraycopy(horarioTurma, 0, todasAsVariaveis, salaTurma.length, horarioTurma.length);

        if ("MRV".equals(nomeHeuristica)) {
            solver.setSearch(Search.minDomLBSearch(todasAsVariaveis));
        } else if ("DOM_W_DEG".equals(nomeHeuristica)) {
            solver.setSearch(Search.domOverWDegSearch(todasAsVariaveis));
        }
        
        String logFilename = "log_" + nomeInstancia + "_" + nomeHeuristica + ".txt";
        try (FileWriter writer = new FileWriter(logFilename)) {
            writer.write("--- RESULTADOS | INSTÂNCIA: " + nomeInstancia + " | HEURÍSTICA: " + nomeHeuristica + " ---\n\n");
            
            if (solver.solve()) {
                writer.write("Solucao encontrada:\n");
                for (int t = 0; t < nTurmas; t++) {
                    writer.write(String.format("Turma %d: Sala = %d, Horario = %d\n", t + 1, salaTurma[t].getValue(), horarioTurma[t].getValue()));
                }
                writer.write("\nViolacao da Restricao C4: " + (c4Violada.getValue() == 1) + "\n");
            } else {
                writer.write("Nenhuma solucao encontrada.\n");
            }
            writer.write("\n--- ESTATÍSTICAS ---\n");
            writer.write("Tempo de resolução: " + solver.getTimeCount() + " segundos\n");
            writer.write("Nós da árvore de busca: " + solver.getNodeCount() + "\n");
            writer.write("Backtracks: " + solver.getBackTrackCount() + "\n");
            writer.write("Falhas: " + solver.getFailCount() + "\n");
            writer.write(solver.getMeasures().toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Processamento concluído. Log salvo em: " + logFilename);
    }
}