# Roteiro de gravação — 9 minutos e 30 segundos

## Preparação

1. Leia o README e execute os testes.
2. Abra o programa com `executar.bat` ou `bash executar.sh`.
3. Aumente a fonte do editor e deixe abertas as classes Dijkstra, AEstrela, EntradaFila e ApoioBusca.
4. Execute uma vez antes de gravar para conhecer os controles.
5. Teste o microfone, feche notificações e grave a tela com legenda legível.
6. Ensaie com cronômetro. A duração abaixo inclui os momentos de demonstração e deixa 30 segundos de margem.

## 00:00–00:35 — Apresentação

**Tela:** interface com o arquivo original, antes de executar.

**Fala:** “Neste vídeo vou apresentar duas implementações em Java para encontrar caminhos em um labirinto: Dijkstra e A*. Vou explicar as decisões de cada algoritmo, mostrar os principais trechos do código e comparar os resultados no arquivo fornecido pelo professor.”

## 00:35–01:25 — Arquivo e regras

**Tela:** `dados/labirinto.dat` e interface.

**Fala:** “O arquivo tem 12 linhas e 10 colunas. Adotei 1 como parede, 0 como passagem, 2 como início e 3 como saída. Existem três saídas. O objetivo padrão é alcançar qualquer uma delas com o menor custo. As coordenadas começam em zero.”

“Cada célula transitável funciona como um vértice. Cada movimento permitido é uma aresta de custo 1. Só podemos subir, descer ou andar para os lados, sem diagonais.”

## 01:25–02:25 — Dijkstra

**Tela:** `Dijkstra.java`, fila e relaxamento; mostrar alguns passos se necessário.

**Fala:** “O Dijkstra prioriza o menor custo acumulado desde o início, chamado de g. A posição inicial recebe custo zero, e as demais começam com infinito.”

“A cada retirada válida da fila, ele finaliza uma posição e analisa os vizinhos. Se o custo atual mais 1 for melhor que o custo conhecido do vizinho, atualiza esse custo e seu predecessor. A busca termina quando uma saída é retirada como a próxima posição válida, e não quando é apenas descoberta.”

## 02:25–03:45 — A* e heurística

**Tela:** `AEstrela.java`, função `heuristica` e cálculo da prioridade.

**Fala:** “O A* acrescenta uma estimativa do custo restante. Sua prioridade é f igual a g mais h. Aqui, h é a distância de Manhattan: diferença entre as linhas mais a diferença entre as colunas.”

“Como há várias saídas, utilizo a menor estimativa entre todas elas. No início, os valores são 11, 7 e 14. A menor estimativa aponta para uma saída isolada. A heurística não conhece o caminho real; ela ignora paredes.”

“Mesmo assim, ela é admissível e consistente para este modelo, porque não superestima a distância real até uma saída e respeita a relação entre vizinhos. Isso permite manter a garantia de caminho ótimo. Usar apenas h seria busca gulosa; aqui usamos g mais h.”

## 03:45–05:10 — Código e correção

**Tela:** trechos preparados de `EntradaFila`, `Dijkstra`, `AEstrela` e `ApoioBusca.reconstruir`.

**Fala:** “Usei a PriorityQueue padrão do Java como estrutura de dados. A lógica dos dois algoritmos está implementada nestas classes.”

“As entradas da fila são imutáveis. Quando um custo melhora, adiciono uma nova entrada; entradas antigas são descartadas na retirada. Assim não altero a prioridade de um objeto que já está dentro da fila.”

“Ambos usam os mesmos vizinhos e desempate por ordem de inserção. O predecessor permite reconstruir o caminho: parto da saída, sigo até o início e inverto a sequência.”

Não leia o código inteiro. Aponte prioridade, relaxamento, descarte, condição de parada e reconstrução.

## 05:10–06:40 — Execução visual

**Tela:** selecione “Qualquer saída (menor custo)”, clique em “Executar ambos”. Ajuste o intervalo para caber no tempo. Pause em um ponto interessante e avance um passo.

**Fala:** “Amarelo representa posições descobertas que aguardam processamento, azul representa posições processadas e roxo mostra o caminho final. O marcador I é o início e S representa uma saída.”

“As duas buscas encontraram a saída em (5, 9), com custo de 13 movimentos. O caminho contém 14 células porque inclui o início.”

“Os caminhos não são idênticos, mas têm o mesmo custo ótimo. A animação representa eventos e não mede tempo de processamento.”

## 06:40–07:40 — Comparação

**Tela:** métricas finais ou CSV da sua execução.

| Métrica padrão | Dijkstra | A* |
|---|---:|---:|
| Custo | 13 | 13 |
| Posições no caminho | 14 | 14 |
| Processadas | 30 | 28 |
| Descobertas | 32 | 30 |

**Fala:** “Dijkstra processou 30 células e A* processou 28. A diferença é pequena neste labirinto, mas mostra o efeito da heurística nesta execução. O Dijkstra prioriza g, e o A* prioriza g mais h.”

“O tempo exibido exclui leitura e animação, mas inclui o registro dos eventos. O console também permite medir sem eventos, após aquecimento. Como o mapa é pequeno, não podemos generalizar a diferença de tempo para qualquer situação.”

Se mostrar tempos, use os valores obtidos no seu computador. Não trate o CSV anexado como uma previsão de desempenho.

## 07:40–08:35 — Saída inacessível

**Tela:** escolha “Somente (8, 0)” e execute ambos com intervalo curto.

**Fala:** “Esta saída está isolada por paredes. Ao selecioná-la como único objetivo, as outras saídas continuam transitáveis, mas não encerram a busca.”

“Os dois algoritmos esgotam as 53 células alcançáveis e retornam sem caminho. Isso mostra que o programa também trata a ausência de solução.”

## 08:35–09:10 — Testes e relação entre algoritmos

**Tela:** saída dos testes; destaque o construtor `new AEstrela(true)` no teste.

**Fala:** “Validei o arquivo original e comparei as buscas com uma referência independente em 400 mapas aleatórios. Também conferi paredes, movimentos, custo, predecessores e arquivos inválidos.”

“Quando a heurística do A* é zero, sua prioridade se torna apenas g. Com o mesmo desempate, ele passa a apresentar o mesmo comportamento do Dijkstra.”

## 09:10–09:30 — Encerramento

**Tela:** retorne ao resultado padrão, já preparado, ou à tabela.

**Fala:** “As implementações encontraram o menor custo no labirinto fornecido. Dijkstra organizou a exploração pelo custo acumulado, enquanto A* utilizou também a distância de Manhattan. A comparação mostrou caminhos ótimos, esforço de exploração e tratamento de uma saída inacessível.”

## Após gravar

- Assista ao vídeo e confira duração máxima de 10 minutos, áudio e legibilidade.
- Publique no YouTube. Se o professor permitir, pode usar “não listado”.
- Teste o link sem sua conta conectada e envie-o no GSA.
- Título sugerido: “A* e Dijkstra em Java: implementação e comparação em um labirinto”.

**Descrição sugerida:** “Implementação própria dos algoritmos A* e Dijkstra em Java, usando o arquivo labirinto.dat fornecido pelo professor. Movimentos ortogonais com custo unitário e heurística de Manhattan para múltiplas saídas. Apresentação do código, execução visual e comparação dos resultados. Aluno: [nome]. Disciplina: [disciplina]. Professor: [nome].”
