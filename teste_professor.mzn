% teste_professor.mzn
int: NTURMAS;
array[1..NTURMAS] of int: professor_turma;

% Variáveis: Apenas os horários, que podem ser 1 ou 2.
array[1..NTURMAS] of var 1..2: horario_turma;

% A única e mais importante restrição do seu modelo:
% Se os professores são iguais, os horários DEVEM ser diferentes.
constraint forall(t1, t2 in 1..NTURMAS where t1 < t2) (
    (professor_turma[t1] = professor_turma[t2]) -> (horario_turma[t1] != horario_turma[t2])
);

solve satisfy;

output ["Horarios encontrados: ", show(horario_turma), "\n"];