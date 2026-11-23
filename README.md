# A* e Dijkstra em Java — labirinto

Projeto completo para implementar, executar, visualizar e comparar os dois algoritmos no arquivo fornecido pelo professor.

## Comece aqui

1. Extraia **todo o ZIP** para uma pasta. Não execute o programa de dentro do ZIP.
2. Tenha Java 17 ou superior instalado. Para alterar/compilar os fontes, use um **JDK completo** 17 ou superior, com o comando `java` no PATH.
3. No Windows, abra `executar.bat`. No Linux/macOS, abra um terminal na pasta e execute `bash executar.sh`.
4. Na interface, mantenha `Qualquer saída (menor custo)` e clique em **Executar ambos**.

O arquivo `labirinto.jar` já está compilado e incluído. Os fontes Java também estão completos. Swing faz parte do Java; não é preciso Maven, Gradle ou baixar bibliotecas.

Para conferir o Java no terminal:

```text
java -version
```

## Interface espacial no navegador

No Linux/macOS, execute na pasta do projeto:

```bash
bash executar.sh --web
```

No Windows: `executar.bat --web`. Abra **http://127.0.0.1:8081** no navegador.
O servidor funciona localmente; encerre com `Ctrl+C` no terminal. A porta padrão
8081 evita o conflito identificado com um servidor HTTP do Windows na porta 8080.
Os scripts de execução usam IPv4 para compatibilidade com o encaminhamento local
entre Windows e WSL. Para outra porta:
`bash executar.sh --web --porta 8082`.

A interface **Órbita** é uma carta estelar 2D vista de cima. A nave exploradora,
inspirada nas silhuetas de Star Trek, parte do setor Sol e navega entre cinturões de
asteroides e buracos negros até mundos como Proxima b, no sistema Alpha Centauri.
A carta é artística, sem escala astronômica; Tau Ceti é representado por uma estação.

O cenário inicial **Fronteira solar** oferece espaço aberto e passagens alternativas.
Use **Região do espaço → Mapa original do projeto** para visualizar o arquivo do
professor, preservado com suas dimensões, regras e destinos inacessíveis.
Ambos os tipos de obstáculo bloqueiam setores: não há atração gravitacional nem
custos extras. Cada movimento ortogonal custa 1.

- Escolha Dijkstra, A* ou a comparação dos dois e clique em **Iniciar exploração**.
- A busca aparece como um scanner; a nave percorre o caminho encontrado com
  interpolação de posição, rotação e rastro de propulsão. A ausência de rota é
  informada sem iniciar um voo.
- Alterne as abas sobre o mapa para acompanhar cada algoritmo na comparação.
- Pause também durante o voo, avance um evento por vez ou reinicie a missão.
- Escolha o destino no seletor ou clique em um planeta no mapa.
- Arraste para deslocar a carta. Use os botões ou a roda do mouse para zoom;
  **⌖** centraliza. Com o mapa focado, use setas, **+**, **−** e **0**.
- **Ambiente vivo** controla a animação de estrelas, detritos e discos de acreção,
  inclusive enquanto nenhuma busca está em execução. A preferência do sistema
  por movimento reduzido desativa o ambiente e evita a interpolação do voo.
- **Grade de setores** exibe as posições e a ocupação exata dos obstáculos.
- Use **Importar mapa .dat** para abrir outro mapa de até 64 KB no formato original
  (`0` passagem, `1` parede, `2` início único e `3` destino).

Os algoritmos são executados em Java; o JavaScript desenha e reproduz os eventos
no canvas. As métricas de tempo excluem a animação. Comparar a duração das animações
não mede o desempenho dos algoritmos.

Arquivos da interface: `web/index.html`, `web/style.css` e `web/app.js`.
O servidor está em `src/labirinto/ServidorWeb.java`. Os arquivos web são incluídos
no JAR: após editar Java, HTML, CSS ou JavaScript, execute `bash compilar.sh`
(Windows: `compilar.bat`) e reinicie o servidor. Não há dependências externas.
A execução sem `--web` continua abrindo a interface Swing.

## Interface para o vídeo

- **Dijkstra / A* / Executar ambos:** calculam a busca e reproduzem o histórico.
- **Pausar / continuar:** controla a reprodução.
- **Um passo:** pausa e aplica o próximo evento de cada painel ativo.
- **Reiniciar:** limpa a visualização e as métricas.
- **Intervalo:** controla os milissegundos entre eventos; menor valor = reprodução mais rápida.
- **Objetivo:** escolhe qualquer saída ou uma saída específica.
- **Carregar arquivo:** abre outro mapa no mesmo formato.

Os painéis avançam por eventos, não por tempo de CPU. Terminar a animação primeiro **não é uma medição de velocidade computacional**. Os resultados finais aparecem ao terminar a reprodução de cada painel.

Legenda: parede cinza; passagem branca; fronteira amarela; processada azul; caminho roxo; marcador I verde = início; S vermelho = saída. O contorno laranja identifica a posição do evento atual. Coordenadas começam em zero.

## Executar pelo terminal

Na pasta extraída:

```text
java -jar labirinto.jar --console
java -jar labirinto.jar --console --objetivo 8,0
java -jar labirinto.jar --console --objetivo 10,7
java -jar labirinto.jar --console --benchmark --csv resultados.csv
java -jar labirinto.jar --arquivo "dados/labirinto.dat" --console
java -jar labirinto.jar --ajuda
```

`--csv` e `--benchmark` ativam o modo console. `--objetivo` requer esse modo. O arquivo é localizado relativamente à pasta de execução; os scripts entram automaticamente na pasta do projeto.

Se executar o JAR a partir de outra pasta, forneça `--arquivo` com o caminho correto.

## Recompilar depois de alterar o código

Windows, no terminal:

```text
compilar.bat
executar.bat
```

Linux/macOS:

```bash
bash compilar.sh
bash executar.sh
```

Os scripts usam os módulos oficiais `jdk.compiler` e `jdk.jartool`, com `--release 17`, para gerar classes compatíveis com Java 17. Um JRE reduzido pode executar o JAR, mas não recompilar; nesse caso, utilize um JDK completo.

**Importante:** `executar` só compila automaticamente quando o JAR não existe. Após editar os fontes, execute `compilar` para atualizar o JAR.

## Rodar os testes

Windows:

```text
testar.bat
```

Linux/macOS:

```bash
bash testar.sh
```

Os testes não exigem bibliotecas e não precisam de ambiente gráfico. Incluem:

- Arquivo original: qualquer saída e cada saída específica.
- Caminhos: limites, paredes, adjacência, início, objetivo e custo.
- Mapas pequenos, saídas empatadas e ausência de solução.
- Validação de caracteres, dimensões, início e saídas.
- CRLF, BOM UTF-8 e arquivo sem quebra final.
- 400 mapas aleatórios com semente fixa e comparação com BFS independente, apenas nos testes.
- A* com `h=0`: mesmo caminho, métricas e ordem de processamento do Dijkstra.
- Consistência da heurística em cada aresta dos mapas testados.
- Desenho dos painéis Swing em imagem, no modo headless.

A construção dos painéis foi exercitada em modo headless. A abertura e interação da janela completa devem ser conferidas no seu computador antes da gravação.

## Formato e regras

O arquivo original está em `dados/labirinto.dat`, preservado byte a byte.

| Caractere | Interpretação |
|---|---|
| 0 | Passagem |
| 1 | Parede |
| 2 | Início único |
| 3 | Saída; pode haver várias |

Essa legenda é a convenção adotada a partir do arquivo: se o professor forneceu outra, ela prevalece.

O leitor descobre as dimensões, não usa cabeçalho, não fixa coordenadas e aceita apenas uma grade retangular não vazia. Linhas vazias e espaços dentro do mapa são rejeitados.

Movimentos: cima, direita, baixo e esquerda, nessa ordem. Sem diagonais, custo 1. O objetivo padrão é alcançar qualquer saída com o menor custo. No modo de saída específica, os outros `3` permanecem transitáveis, mas não encerram a busca.

## Resultados reproduzíveis no arquivo original

| Objetivo | Algoritmo | Custo | Células no caminho | Processadas | Descobertas |
|---|---|---:|---:|---:|---:|
| Qualquer saída | Dijkstra | 13 | 14 | 30 | 32 |
| Qualquer saída | A* | 13 | 14 | 28 | 30 |
| Somente (5, 9) | Dijkstra | 13 | 14 | 30 | 32 |
| Somente (5, 9) | A* | 13 | 14 | 24 | 26 |
| Somente (8, 0) | Dijkstra | Sem caminho | 0 | 53 | 53 |
| Somente (8, 0) | A* | Sem caminho | 0 | 53 | 53 |
| Somente (10, 7) | Dijkstra | 22 | 23 | 46 | 47 |
| Somente (10, 7) | A* | 22 | 23 | 44 | 46 |

Os números dependem da ordem de vizinhos, da heurística e do desempate documentados. Tempos variam entre máquinas e execuções. Verifique a tabela por execução antes de apresentar; o CSV anexado contém a execução padrão realizada neste ambiente.

Dois caminhos diferentes podem ter o mesmo custo ótimo. Isso acontece nas implementações entregues: ambos chegam a `(5, 9)` com 13 movimentos, mas percorrem trechos iniciais diferentes.

## O que cada arquivo faz

| Arquivo em `src/labirinto` | Função |
|---|---|
| Main.java | Entrada, argumentos, console, benchmark e CSV |
| Posicao.java | Coordenada imutável |
| Labirinto.java | Matriz, validação e vizinhos |
| LeitorLabirinto.java | Leitura UTF-8 |
| AlgoritmoBusca.java | Contrato comum |
| Dijkstra.java | Implementação própria de Dijkstra |
| AEstrela.java | Implementação própria de A* e heurística |
| EntradaFila.java | Snapshot e comparação da fila |
| ApoioBusca.java | Inicialização, validação de objetivos e reconstrução |
| ResultadoBusca.java | Caminho, métricas e eventos |
| EventoBusca.java | Histórico para animação |
| JanelaLabirinto.java | Controles e reprodução |
| PainelLabirinto.java | Desenho da grade |

Documentação complementar:

- `docs/EXPLICACAO_TECNICA.md`: funcionamento e decisões de correção.
- `docs/ROTEIRO_VIDEO.md`: roteiro de gravação de 9min30s.
- `docs/VALIDACAO.md`: verificações realizadas e limites.
- `docs/resultados-original.csv`: métricas de uma execução real, com benchmark.

## Problemas comuns

| Problema | Como resolver |
|---|---|
| `java` não reconhecido | Instale/configure Java no PATH e reabra o terminal |
| `UnsupportedClassVersionError` | Use Java 17 ou superior |
| `jdk.compiler` indisponível ou `release 17 not supported` | Para recompilar, utilize um JDK completo 17 ou superior |
| Arquivo não encontrado | Extraia tudo e use o script, ou forneça `--arquivo` |
| Janela não abre em servidor/terminal remoto | Use `--console`; a janela exige ambiente gráfico |
| Alterei o código e nada mudou | Execute `compilar` para atualizar o JAR |
| Programa informa sem caminho | Confira o objetivo; `(8, 0)` é realmente inacessível |

## Entrega da atividade

Grave a explicação e a execução em até 10 minutos, publique no YouTube e envie o link no GSA. O projeto e os testes estão prontos; a gravação, publicação e envio ainda precisam ser realizados pelo aluno.
