package labirinto;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.Timer;

@SuppressWarnings("serial") // A janela não é persistida via serialização Java.
public final class JanelaLabirinto extends JFrame {
    private static final long serialVersionUID = 1L;
    private Labirinto mapa;
    private final PainelLabirinto[] paineis = new PainelLabirinto[2];
    private final JTextArea[] informacoes = new JTextArea[2];
    private final ResultadoBusca[] resultados = new ResultadoBusca[2];
    private final int[] cursores = new int[2];
    private final int[] processadas = new int[2], descobertas = new int[2];
    private final JComboBox<String> objetivos = new JComboBox<>();
    private final JLabel arquivoLabel = new JLabel();
    private final JButton carregar = new JButton("Carregar arquivo");
    private final JButton dijkstra = new JButton("Dijkstra");
    private final JButton aestrela = new JButton("A*");
    private final JButton ambos = new JButton("Executar ambos");
    private final JButton pausar = new JButton("Pausar / continuar");
    private final JButton passo = new JButton("Um passo");
    private final JButton reiniciar = new JButton("Reiniciar");
    private final Timer timer;
    private boolean calculando;

    public JanelaLabirinto(Labirinto mapa, Path arquivo) {
        super("A* e Dijkstra em Java — comparação de caminhos");
        this.mapa = mapa;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        JPanel topo = new JPanel(new GridLayout(0, 1));
        arquivoLabel.setBorder(BorderFactory.createEmptyBorder(7, 12, 4, 12));
        topo.add(arquivoLabel);
        JPanel linha1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        linha1.add(carregar);
        linha1.add(new JLabel("Objetivo:"));
        linha1.add(objetivos);
        linha1.add(dijkstra); linha1.add(aestrela); linha1.add(ambos);
        topo.add(linha1);
        JPanel linha2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        linha2.add(pausar); linha2.add(passo); linha2.add(reiniciar);
        linha2.add(new JLabel("Intervalo (ms):"));
        JSlider velocidade = new JSlider(10, 300, 60);
        velocidade.setPreferredSize(new Dimension(160, 42));
        velocidade.setMajorTickSpacing(145); velocidade.setPaintLabels(true);
        linha2.add(velocidade);
        topo.add(linha2);
        add(topo, BorderLayout.NORTH);
        JPanel centro = new JPanel(new GridLayout(1, 2, 10, 0));
        for (int i = 0; i < 2; i++) {
            JPanel coluna = new JPanel(new BorderLayout());
            coluna.setBorder(BorderFactory.createTitledBorder(i == 0 ? "Dijkstra: prioridade = g" : "A*: prioridade = g + h"));
            paineis[i] = new PainelLabirinto(mapa);
            coluna.add(paineis[i], BorderLayout.CENTER);
            informacoes[i] = new JTextArea(5, 35);
            informacoes[i].setEditable(false);
            informacoes[i].setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            informacoes[i].setBackground(new Color(245, 247, 251));
            informacoes[i].setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
            coluna.add(informacoes[i], BorderLayout.SOUTH);
            centro.add(coluna);
        }
        add(centro, BorderLayout.CENTER);
        JLabel legenda = new JLabel("  Cinza: parede | Branco: passagem | Amarelo: fronteira | Azul: processada | Roxo: caminho | I: início | S: saída");
        legenda.setBorder(BorderFactory.createEmptyBorder(8, 0, 12, 0));
        add(legenda, BorderLayout.SOUTH);
        timer = new Timer(60, e -> avancar());
        velocidade.addChangeListener(e -> timer.setDelay(velocidade.getValue()));
        carregar.addActionListener(e -> carregar());
        dijkstra.addActionListener(e -> executar(true, false));
        aestrela.addActionListener(e -> executar(false, true));
        ambos.addActionListener(e -> executar(true, true));
        reiniciar.addActionListener(e -> limpar());
        objetivos.addActionListener(e -> { if (!calculando) limpar(); });
        pausar.addActionListener(e -> {
            if (timer.isRunning()) timer.stop();
            else if (pendente()) timer.start();
        });
        passo.addActionListener(e -> { timer.stop(); avancar(); });
        preencherObjetivos();
        atualizarArquivo(arquivo);
        limpar();
        setMinimumSize(new Dimension(1000, 700));
        setSize(1160, 880);
        setLocationRelativeTo(null);
    }

    private void preencherObjetivos() {
        objetivos.removeAllItems();
        objetivos.addItem("Qualquer saída (menor custo)");
        for (Posicao p : mapa.saidas()) objetivos.addItem("Somente " + p);
    }
    private void atualizarArquivo(Path arquivo) {
        arquivoLabel.setText("Arquivo: " + arquivo.toAbsolutePath() + " | Grade: " + mapa.linhas() + " × " + mapa.colunas()
                + " | Início: " + mapa.inicio() + " | Saídas: " + mapa.saidas());
    }
    private void limpar() {
        timer.stop();
        for (int i = 0; i < 2; i++) {
            resultados[i] = null; cursores[i] = 0; processadas[i] = 0; descobertas[i] = 0;
            paineis[i].reiniciar(mapa);
            informacoes[i].setText("Aguardando execução.\nCoordenadas: (linha, coluna), iniciadas em zero.");
        }
    }
    private List<Posicao> metas() {
        int indice = objetivos.getSelectedIndex();
        return indice <= 0 ? mapa.saidas() : List.of(mapa.saidas().get(indice - 1));
    }
    private void bloquear(boolean bloquear) {
        calculando = bloquear;
        carregar.setEnabled(!bloquear); dijkstra.setEnabled(!bloquear); aestrela.setEnabled(!bloquear);
        ambos.setEnabled(!bloquear); objetivos.setEnabled(!bloquear); reiniciar.setEnabled(!bloquear);
        passo.setEnabled(!bloquear); pausar.setEnabled(!bloquear);
    }
    private void executar(boolean usarDijkstra, boolean usarA) {
        limpar();
        List<Posicao> metas = metas();
        bloquear(true);
        for (JTextArea info : informacoes) info.setText("Calculando o histórico da busca...");
        // Não bloqueia a thread da interface. As buscas são independentes e sequenciais.
        new SwingWorker<ResultadoBusca[], Void>() {
            @Override protected ResultadoBusca[] doInBackground() {
                return new ResultadoBusca[] {
                    usarDijkstra ? new Dijkstra().buscar(mapa, metas, true) : null,
                    usarA ? new AEstrela().buscar(mapa, metas, true) : null
                };
            }
            @Override protected void done() {
                try {
                    ResultadoBusca[] calculados = get();
                    System.arraycopy(calculados, 0, resultados, 0, 2);
                    for (int i = 0; i < 2; i++) informacoes[i].setText(resultados[i] == null ? "Não executado." : "Reproduzindo busca...");
                    timer.start();
                } catch (Exception erro) {
                    JOptionPane.showMessageDialog(JanelaLabirinto.this, erro.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                } finally { bloquear(false); }
            }
        }.execute();
    }
    private boolean pendente() {
        for (int i = 0; i < 2; i++) if (resultados[i] != null && cursores[i] < resultados[i].eventos().size()) return true;
        return false;
    }
    private void avancar() {
        for (int i = 0; i < 2; i++) {
            ResultadoBusca r = resultados[i];
            if (r == null || cursores[i] >= r.eventos().size()) continue;
            EventoBusca evento = r.eventos().get(cursores[i]++);
            paineis[i].aplicar(evento);
            if (evento.tipo() == EventoBusca.Tipo.PROCESSADA) processadas[i]++;
            if (evento.tipo() == EventoBusca.Tipo.DESCOBERTA) descobertas[i]++;
            if (cursores[i] == r.eventos().size()) informacoes[i].setText(resumo(r));
            else informacoes[i].setText("Evento: " + evento.tipo() + " em " + evento.posicao()
                    + "\ng=" + evento.g() + "  h=" + evento.h() + "  prioridade="
                    + (i == 0 ? evento.g() : evento.f()) + "\nProcessadas: " + processadas[i]
                    + " | Descobertas: " + descobertas[i]
                    + "\nPasso " + cursores[i] + " de " + r.eventos().size());
        }
        if (!pendente()) timer.stop();
    }
    public static String resumo(ResultadoBusca r) {
        return (r.encontrou() ? "Saída: " + r.destino() + " | Custo/movimentos: " + r.custo()
                + "\nCélulas no caminho: " + r.caminho().size() : "Sem caminho para o objetivo selecionado.")
                + "\nProcessadas: " + r.processadas() + " | Descobertas: " + r.descobertas()
                + String.format(java.util.Locale.ROOT, "\nBusca com registro: %.3f ms (sem animação)", r.nanos() / 1_000_000.0);
    }
    private void carregar() {
        timer.stop();
        JFileChooser seletor = new JFileChooser(Path.of("dados").toFile());
        if (seletor.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path arquivo = seletor.getSelectedFile().toPath();
        try {
            Labirinto novo = LeitorLabirinto.ler(arquivo);
            mapa = novo;
            preencherObjetivos(); atualizarArquivo(arquivo); limpar();
        } catch (Exception erro) {
            JOptionPane.showMessageDialog(this, erro.getMessage(), "Arquivo inválido", JOptionPane.ERROR_MESSAGE);
        }
    }
}
