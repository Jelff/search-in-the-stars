# Funcionamento das implementações

## Modelo do problema

Cada célula transitável é um vértice e cada movimento ortogonal permitido é uma aresta de custo 1. Não há movimentos diagonais nem pesos negativos. Os números no arquivo são tipos de célula, não pesos.

Os dois algoritmos recebem o mesmo mapa imutável e a mesma lista de objetivos. Cada execução cria suas próprias distâncias, predecessores, fila e conjunto fechado. Uma busca não reutiliza os estados da outra.

## Dijkstra

A prioridade de uma posição é `g(n)`, o melhor custo acumulado conhecido a partir do início.

1. Distâncias começam em infinito; a distância do início é zero.
2. A fila seleciona a menor prioridade.
3. Entradas desatualizadas ou já finalizadas são ignoradas.
4. A posição é finalizada e contada como processada.
5. Se for um objetivo, o caminho é reconstruído.
6. Caso contrário, cada aresta é relaxada: se `g(atual) + 1 < g(vizinho)`, atualizar distância e predecessor e inserir uma nova entrada.

Uma posição é finalizada na retirada válida, não quando é descoberta. Custos não negativos justificam que uma posição retirada com menor custo não precise ser reaberta.

## A*

A prioridade é `f(n) = g(n) + h(n)`. `g` tem o mesmo significado usado em Dijkstra.

Para o conjunto de objetivos G:

```text
h(n) = min, para s em G, de |linha(n) - linha(s)| + |coluna(n) - coluna(s)|
```

Com um único objetivo, usa-se apenas a distância a ele. Com várias saídas, utiliza-se o mínimo entre todas as selecionadas. A saída inacessível não é removida antecipadamente.

No início `(2, 1)`, as distâncias às saídas são 11, 7 e 14; logo `h=7`. A saída com estimativa 7 está isolada. Isso não torna a heurística inválida: ignorar paredes pode subestimar a distância real.

### Admissibilidade

Se existe um caminho até uma saída, seu comprimento é pelo menos a distância de Manhattan até essa saída. O mínimo das estimativas até todas as saídas é menor ou igual à distância até a saída ótima. Portanto, não superestima o menor custo real.

### Consistência

Para duas posições vizinhas u e v, cada distância de Manhattan satisfaz `h_s(u) <= 1 + h_s(v)`. Escolhendo a saída que minimiza a distância em v, obtemos `min_s h_s(u) <= 1 + min_s h_s(v)`. Além disso, `h=0` em todo objetivo selecionado.

Por isso, no modelo entregue, o primeiro fechamento de uma posição já utiliza seu melhor custo. O código não reabre posições finalizadas. Se alguém trocar a heurística por outra inconsistente ou mudar as regras de custo/movimento, precisará revisar essa decisão.

### Encerramento

O A* encerra somente quando um objetivo é retirado como entrada válida. A prioridade mínima somada à admissibilidade e consistência garante o custo ótimo nesse modelo.

Selecionar apenas `h` seria busca gulosa. Selecionar `g+h` é o comportamento implementado.

## Fila e desempate

`PriorityQueue` é apenas a estrutura de dados padrão; não executa os algoritmos pelo aluno.

`EntradaFila` é um record imutável com posição, g, prioridade e sequência de inserção. Nunca se modifica a prioridade de um objeto que já esteja na fila. Uma melhoria adiciona outra entrada. Ao retirar, entradas cujo g não corresponde ao melhor g atual são descartadas.

Prioridade menor vem primeiro; empates seguem a sequência de inserção. Os vizinhos são gerados na ordem cima, direita, baixo, esquerda.

## Caminho

O predecessor é atualizado quando existe uma melhoria estrita no custo. Após alcançar o objetivo, a reconstrução percorre os predecessores de trás para frente e inverte a lista.

Se o custo é 13, o caminho contém 14 posições. Outros caminhos de mesmo custo podem existir. Dijkstra e A* não precisam devolver a mesma sequência para estarem corretos.

## Métricas

- **Descobertas:** células distintas vistas pela primeira vez, incluindo o início.
- **Processadas:** células retiradas em entrada válida e finalizadas, incluindo a saída encontrada.
- **Custo:** soma dos custos das arestas; neste projeto, igual ao número de movimentos.
- **Sem solução:** custo `-1`, lista de caminho vazia e destino nulo.
- **Tempo com registro:** inicialização das estruturas, busca, criação dos eventos de exploração e reconstrução do caminho. Exclui leitura do arquivo, eventos finais do desenho do caminho, apresentação, CSV e animação. Não representa uma medição isolada das operações da fila.
- **Benchmark:** 50 aquecimentos e mediana de 101 buscas sem registrar eventos. Ainda inclui inicialização de estruturas e reconstrução. É uma medição didática local, sem controle estatístico de JVM, GC ou sistema operacional.

Não compare o tempo de reprodução dos painéis: cada passo visual representa um evento, não uma unidade de CPU. O custo, o caminho válido e a contagem de células são mais informativos neste mapa pequeno.

## Complexidade

Nesta grade, cada vértice tem até quatro vizinhos; portanto E é O(V). A fila pode ter entradas antigas, mas a quantidade é limitada pelas melhorias geradas nas arestas. Para K objetivos, o cálculo direto da heurística custa O(K) por chamada.

- Dijkstra: O(V log V) nesta grade, com memória O(V).
- A*: limite O(V log V + V K) nesta grade, com memória O(V), sem contar o armazenamento dos K objetivos. Para K fixo, permanece O(V log V).

O A* pode reduzir o trabalho observado ao usar informação sobre o destino, mas isso não garante menor tempo em todo mapa. O cálculo da heurística também tem custo.

## Relação A* e Dijkstra

Com `new AEstrela(true)`, h é zero. A prioridade se torna `g`, e os testes confirmam a mesma ordem de processamento, caminho e métricas do Dijkstra, com o desempate adotado.

## Leitura e apresentação

O leitor aceita UTF-8 com BOM opcional, LF/CRLF e última linha sem quebra. Rejeita arquivo vazio, grade não retangular, símbolos desconhecidos, múltiplos inícios e ausência de início/saída.

`SwingWorker` calcula o histórico sem bloquear a thread de eventos da interface. `javax.swing.Timer` aplica os eventos à visualização. Nenhum `sleep` participa dos algoritmos.
