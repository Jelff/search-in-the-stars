# Validação realizada

- Data: 18/11/2026.
- Ambiente: Linux, OpenJDK 21.0.13, execução de testes em modo headless.
- Compilação local: `-source 17 -target 17 -encoding UTF-8 -Xlint:all,-options -Werror`.
- O runtime local contém o módulo compilador, mas não o arquivo de assinaturas históricas necessário para `--release 17`. Por isso a compilação local usou source/target. Os scripts entregues usam `--release 17`, apropriado para um JDK completo. As APIs utilizadas estão disponíveis em Java 17.
- JAR gerado com versão de classe 61, correspondente ao Java 17.
- 64.710 verificações automatizadas aprovadas, incluindo 400 mapas aleatórios com referência BFS independente.
- Arquivo original preservado sem alterar os bytes.
- Qualquer saída: ambos obtiveram custo 13 e destino (5, 9).
- Saída (8, 0): ambos esgotaram 53 posições e retornaram sem caminho.
- Saída (10, 7): ambos obtiveram custo 22.
- Console, argumentos, CSV, benchmark, leitura e reconstrução exercitados.
- Painéis Swing desenhados em imagens no modo headless e inspecionados.

## Limites da verificação

A janela completa não foi aberta em uma sessão gráfica neste ambiente. Os botões, seletor de arquivos e reprodução devem ser conferidos no computador do aluno antes de gravar. Os scripts `.bat` foram fornecidos para Windows, mas não executados neste ambiente Linux.

Os testes oferecem evidência de implementação; a justificativa da optimalidade vem das propriedades dos algoritmos e da heurística descritas em EXPLICACAO_TECNICA.md. Não há alegação de que testes aleatórios provem correção para todas as entradas.

Os tempos do CSV correspondem a uma execução local e não são resultados universais. O benchmark é didático e não substitui um estudo rigoroso de desempenho.

## Carta estelar web — 20/11/2026

- Interface redesenhada em Canvas 2D, com cenário Fronteira solar, três destinos,
  cinturões de asteroides, duas singularidades e animação ambiente.
- Compilação com `bash testar.sh`, usando `--release 17`: 64.710 verificações Java aprovadas.
- 21 verificações de integração aprovadas em Chromium headless, com Playwright
  instalado temporariamente fora do projeto. Nenhuma dependência adicionada à aplicação.
- Conferidos: carregamento, seleção de planeta por clique, zoom, câmera por teclado
  e arraste, pausa de eventos, passo individual, interpolação e pausa durante o voo,
  chegada das duas naves, reinício e apresentação das métricas.
- Os três destinos do cenário novo foram consultados na API: ambos os algoritmos
  encontraram rotas com custo mínimo igual para cada destino.
- Conferidos o destino inacessível `(8, 0)` do arquivo original, importação válida,
  retorno ao mapa importado após trocar o cenário e preservação do mapa após erro
  de importação.
- Conferidos o desligamento da animação ambiente, a preferência por movimento
  reduzido e o layout de 390 px sem rolagem horizontal.
- Capturas da interface em desktop e mobile inspecionadas. Nenhum erro JavaScript
  observado no navegador durante as verificações.

O cenário espacial é uma composição artística, sem escala astronômica. Os discos
dos buracos negros representam obstáculos; não simulam física gravitacional. A
comparação continua usando os algoritmos Java existentes e movimentos ortogonais
de custo 1. O arquivo `dados/labirinto.dat` não foi editado.

## Acesso pelo Windows / WSL — 20/11/2026

- Identificado conflito com `httpd` do Windows na porta 8080.
- Porta padrão alterada para 8081 e JAR recompilado.
- Scripts de execução ajustados com `-Djava.net.preferIPv4Stack=true`: o socket
  IPv6 com endereço IPv4 mapeado não era encaminhado corretamente pelo WSL.
- Interface confirmada com HTTP 200 pelo PowerShell do Windows tanto em
  `http://127.0.0.1:8081/` quanto em `http://localhost:8081/`.
- API de busca consultada pelo Windows com o mapa mínimo `23`: Dijkstra e A*
  retornaram caminho encontrado e custo 1. Interface e início de execução também
  conferidos em Chromium na nova porta.
