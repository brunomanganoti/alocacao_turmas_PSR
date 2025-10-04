/* Alocação de turmas em salas de aula */

/* SEÇÃO 1 */
int: NTURMAS;
int: NSALAS;
int: NHORARIOS;

set of int: TURMAS = 1..NTURMAS;
set of int: SALAS = 1..NSALAS;
set of int: HORARIOS = 1..NHORARIOS;

array[TURMAS] of int: alunos_turma;
array[SALAS] of int: capacidade_sala;
array[TURMAS] of int: professor_turma;

bool: is_optimization_problem;

% Constantes
int: TurmaA;
int: TurmaC;
int: SalaLab1;
int: HorarioH1;

enum HEURISTICA = {
  NONE,     
  MRV,       
  DOM_W_DEG   
};
par HEURISTICA: heuristica_selecionada;

/* SEÇÃO 2: VARIÁVEIS DE DECISÃO */
array[TURMAS] of var SALAS: sala_turma;
array[TURMAS] of var HORARIOS: horario_turma;
var bool: c4_violada;

/* SEÇÃO 3: RESTRIÇÕES */
constraint forall(t in TURMAS) (
    capacidade_sala[sala_turma[t]] >= alunos_turma[t]
);

% Regra Específica C5
constraint if NTURMAS >= TurmaC then
    (horario_turma[TurmaA] = HorarioH1) -> (sala_turma[TurmaA] != sala_turma[TurmaC])
else
    true
endif;

% Regra 1: Duas turmas não podem estar na mesma sala e mesmo horário.
constraint forall(t1, t2 in TURMAS where t1 < t2) (
    (horario_turma[t1] = horario_turma[t2]) -> (sala_turma[t1] != sala_turma[t2])
);

% Regra 2: O mesmo professor não pode dar duas aulas no mesmo horário.
constraint forall(t1, t2 in TURMAS where t1 < t2) (
    (professor_turma[t1] = professor_turma[t2]) -> (horario_turma[t1] != horario_turma[t2])
);

% C4
constraint if NTURMAS >= TurmaC /\ is_optimization_problem then
    (c4_violada <-> sala_turma[TurmaC] = SalaLab1)
else
    ((if NTURMAS >= TurmaC /\ SalaLab1 in SALAS then
        sala_turma[TurmaC] != SalaLab1
     else
        true
     endif)
    /\
    (c4_violada = false))
endif;

var int: objetivo = bool2int(c4_violada);

array[int] of var int: search_vars =
  [sala_turma[t] | t in TURMAS] ++ [horario_turma[t] | t in TURMAS];

ann: busca =
  if heuristica_selecionada == MRV then
    int_search(search_vars, first_fail, indomain_min, complete)
  elseif heuristica_selecionada == DOM_W_DEG then
    int_search(search_vars, dom_w_deg, indomain_min, complete)
  else % NONE
    int_search(search_vars, input_order, indomain_min, complete)
  endif;

solve :: busca minimize objetivo;

output ["Solucao encontrada:\n"] ++
        [ "Turma \(t): Sala = \(sala_turma[t]), Horario = \(horario_turma[t])\n" | t in TURMAS] ++
        ["\nViolacao da Restricao C4 (preferencia): ", show(c4_violada)];