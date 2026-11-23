'use strict';
// Os algoritmos e suas métricas continuam sendo executados pelo servidor Java.
const $ = id => document.getElementById(id);
const canvas = $('space'), ctx = canvas.getContext('2d');
const motionPreference = matchMedia('(prefers-reduced-motion: reduce)');
const cores = ['#74e7d4', '#b69aff'];
const TAU = Math.PI * 2;
let mapa, original, importado, textoMapa = '', resultados = [], estados = [], ativo = 0;
let rodando = false, ocupado = false, acumulado = 0, ultimoFrame = 0, tempoAmbiente = 0;
let cenario = 'solar', objetos = [], planetas = [], geometria, hover = null, arraste = null;
let largura = 0, altura = 0, fundo, fundoSujo = true;
const camera = {zoom:1, x:0, y:0};
const catalogo = [
  {nome:'Proxima b', sistema:'ALPHA CENTAURI', cor:'#d4b988', secundaria:'#80644c'},
  {nome:'Épsilon Eridani b', sistema:'ÉPSILON ERIDANI', cor:'#8acdcc', secundaria:'#325f84'},
  {nome:'Tau Ceti · estação', sistema:'SISTEMA TAU CETI', cor:'#bca1e0', secundaria:'#615983'}
];
const aleatorio = n => { const v = Math.sin(n * 127.1 + 43.7) * 43758.5453; return v - Math.floor(v); };
const igual = (a,b) => a && b && a[0] === b[0] && a[1] === b[1];
function anunciar(texto) { $('status').textContent = texto; }
function estadoVazio() {
  return {cursor:0, celulas:new Map(), rota:[], atual:null, processadas:0, descobertas:0,
    nave:mapa ? [...mapa.inicio] : [0,0], angulo:Math.PI / 2, movimento:null, finalizado:false, chegada:null};
}
function pendente() { return resultados.some((r,i) => r && !estados[i].finalizado); }
function limpar() {
  rodando = false; acumulado = 0; resultados = []; estados = [estadoVazio(), estadoVazio()];
  [0,1].forEach(i => {
    const card = $('result'+i);
    card.querySelector('.result-state').textContent = 'Aguardando missão';
    card.querySelectorAll('dd').forEach(d => d.textContent = '—');
    card.querySelector('.progress-fill').style.width = '0%';
  });
  $('coordinates').textContent = 'ARRASTE PARA EXPLORAR · CLIQUE EM UM PLANETA';
  atualizarHud(); botoes();
}
function botoes() {
  $('launch').disabled = !mapa || ocupado;
  $('pause').disabled = !pendente() || ocupado;
  $('step').disabled = !pendente() || ocupado;
  $('pause').textContent = rodando ? 'Pausar' : 'Continuar';
  ['load','reset','algorithm','target','scenario'].forEach(id => $(id).disabled = ocupado);
}
function criarCenario() {
  const linhas = 22, colunas = 34;
  const matriz = Array.from({length:linhas}, () => Array(colunas).fill('0'));
  // Cinturões descontínuos: vazios legíveis e mais de uma rota entre os sistemas.
  for (let l=2; l<20; l++) {
    const c = Math.round(12 + Math.sin(l*.35)*3);
    if (![6,7,15,16].includes(l)) { matriz[l][c]='1'; if(l%3) matriz[l][c+1]='1'; }
  }
  for(let c=18;c<32;c++) {
    const l = Math.round(13 + Math.sin(c*.43)*2);
    if(c!==23 && c!==24 && c!==29) matriz[l][c]='1';
  }
  [[3,5],[4,6],[7,4],[10,7],[16,5],[19,10],[2,21],[3,23],[7,27],[5,29],[17,29],[19,26],[18,18],[2,16],[9,19],[17,9]].forEach(([l,c]) => matriz[l][c]='1');
  // O disco inteiro de cada singularidade ocupa setores bloqueados e visíveis.
  for(const [l,c] of [[6,20],[17,22]]) {
    for(let dl=-1;dl<=1;dl++) for(let dc=-1;dc<=1;dc++) matriz[l+dl][c+dc]='1';
  }
  matriz[17][3]='2';
  const saidas = [[4,30],[18,31],[3,10]];
  saidas.forEach(([l,c]) => matriz[l][c]='3');
  return {linhas:matriz.map(l => l.join('')), inicio:[17,3], saidas};
}
function prepararObjetos() {
  objetos=[];
  const nucleos = cenario==='solar' ? [[6,20],[17,22]] : [];
  for(let l=0;l<mapa.linhas.length;l++) for(let c=0;c<mapa.linhas[0].length;c++) {
    if(mapa.linhas[l][c]!=='1') continue;
    const nucleo = nucleos.find(([nl,nc]) => Math.abs(nl-l)<=1 && Math.abs(nc-c)<=1);
    if(nucleo) {
      if(igual(nucleo,[l,c])) objetos.push({tipo:'buraco',p:[l,c],r:1.28,seed:l*43+c*17});
      continue;
    }
    const seed=l*43+c*17;
    objetos.push({tipo:cenario!=='solar' && seed%13===0?'buraco':'rocha',p:[l,c],r:.38,seed});
  }
  planetas=mapa.saidas.map((p,i) => ({...catalogo[i%catalogo.length],nome:i<catalogo.length?catalogo[i].nome:`Kepler · posto ${i+1}`, p}));
  hover=null; $('tooltip').hidden=true;
}
function usarMapa(m, texto, nome) {
  mapa=m; textoMapa=texto; prepararObjetos(); limpar(); centralizar();
  $('target').replaceChildren(new Option('Destino acessível mais próximo',''));
  planetas.forEach(p => $('target').add(new Option(p.nome+' · '+p.p.join(', '), p.p.join(','))));
  $('dimensions').textContent=`${m.linhas.length} × ${m.linhas[0].length} setores`;
  $('filename').textContent=nome;
  $('regionName').textContent=cenario==='solar'?'FRONTEIRA SOLAR':cenario==='original'?'SETOR ORIGINAL':'SETOR IMPORTADO';
  anunciar('Central pronta. Escolha um planeta ou explore a rota até o destino mais próximo.');
}
async function resposta(req) {
  const r=await req;
  if(!r.ok) throw new Error((await r.text()).slice(0,300));
  return r.json();
}
async function iniciar() {
  limpar(); ocupado=true; botoes(); anunciar('Calculando rotas na central Java…');
  try {
    const destino=$('target').value;
    const dados=await resposta(fetch('/api/buscar'+(destino?'?objetivo='+encodeURIComponent(destino):''),{
      method:'POST',headers:{'Content-Type':'text/plain; charset=utf-8'},body:textoMapa
    }));
    resultados=dados.resultados.map((r,i) => $('algorithm').value==='ambos'||$('algorithm').value===String(i)?r:null);
    if($('algorithm').value!=='ambos') selecionar(Number($('algorithm').value));
    resultados.forEach((r,i) => $('result'+i).querySelector('.result-state').textContent=r?'Scanner ativo · procurando uma rota':'Não selecionado');
    rodando=true; acumulado=0;
    anunciar('Scanner ativo. A fronteira avança; a nave partirá quando a rota for encontrada.');
  } catch(e) { anunciar('Não foi possível iniciar: '+e.message); }
  finally { ocupado=false; botoes(); atualizarHud(); }
}
function selecionar(i) {
  ativo=i;
  [0,1].forEach(j => {
    $('tab'+j).classList.toggle('selected',i===j);
    $('tab'+j).setAttribute('aria-pressed',String(i===j));
  });
  $('mapMode').textContent=i===0?'DIJKSTRA · g':'A ESTRELA · g + h';
  atualizarHud();
}
function atualizarHud() {
  const s=estados[ativo], r=resultados[ativo];
  let fase='Em órbita de espera', detalhe='Propulsão em espera · scanner disponível';
  if(ocupado) { fase='Conectando à central'; detalhe='Calculando a navegação em Java'; }
  else if(r) {
    if(s.finalizado) {
      fase=r.encontrou?'Destino alcançado':'Nenhuma rota segura';
      const planeta=planetas.find(p => igual(p.p,s.nave));
      detalhe=r.encontrou?`${planeta?.nome || 'Destino'} · ${r.custo} saltos`:'Os setores bloqueados impedem a passagem';
    } else if(s.rota.length) {
      fase=rodando?'Em curso · propulsão ativa':'Voo em pausa';
      detalhe='Rota segura confirmada · seguindo coordenadas';
    } else {
      fase=rodando?'Mapeando o desconhecido':'Scanner em pausa';
      detalhe=`${s.processadas} setores explorados · ${s.descobertas} descobertos`;
    }
  }
  $('flightPhase').textContent=fase; $('flightDetail').textContent=detalhe;
  if(s?.atual) $('coordinates').textContent=`SETOR ${s.atual.posicao.join(' / ')} · g ${s.atual.g} · h ${s.atual.h}`;
}
function finalizar(i) {
  const s=estados[i],r=resultados[i];
  if(!r || s.finalizado || s.movimento || s.cursor<r.eventos.length) return;
  s.finalizado=true; s.chegada=tempoAmbiente;
  const card=$('result'+i);
  card.querySelector('.result-state').textContent=r.encontrou?'Destino alcançado · rota segura':'Destino inacessível · sem caminho';
  const valores=[r.encontrou?r.custo:'—',r.processadas,r.descobertas,(r.nanos/1e6).toFixed(3)+' ms'];
  card.querySelectorAll('dd').forEach((d,j) => d.textContent=valores[j]);
  card.querySelector('.progress-fill').style.width='100%';
}
function verificarConclusao() {
  if(resultados.length && !pendente()) {
    rodando=false;
    const sucesso=resultados.some(r => r?.encontrou);
    anunciar(sucesso?'Missão concluída. Compare o custo da rota e os setores explorados pelos algoritmos.':'Exploração concluída. Nenhum dos algoritmos encontrou passagem até esse destino.');
  }
  botoes(); atualizarHud();
}
function avancar(passo=false) {
  resultados.forEach((r,i) => {
    if(!r) return;
    const s=estados[i];
    if(passo && s.movimento) { s.nave=[...s.movimento.para]; s.movimento=null; }
    if(s.movimento || s.cursor>=r.eventos.length) { finalizar(i); return; }
    const evento=r.eventos[s.cursor++]; s.atual=evento;
    s.celulas.set(evento.posicao.join(','),evento.tipo);
    if(evento.tipo==='DESCOBERTA') s.descobertas++;
    if(evento.tipo==='PROCESSADA') s.processadas++;
    if(evento.tipo==='CAMINHO') {
      s.rota.push(evento.posicao);
      if(!igual(s.nave,evento.posicao)) {
        const dr=evento.posicao[0]-s.nave[0],dc=evento.posicao[1]-s.nave[1];
        const angulo=Math.atan2(dr,dc)+Math.PI/2;
        if(passo || motionPreference.matches) { s.nave=[...evento.posicao]; s.angulo=angulo; }
        else s.movimento={de:[...s.nave],para:[...evento.posicao],tempo:0,duracao:Math.max(75,Number($('speed').value)*5),angulo};
      }
    }
    const card=$('result'+i), valores=card.querySelectorAll('dd');
    valores[1].textContent=s.processadas; valores[2].textContent=s.descobertas;
    if(evento.tipo==='CAMINHO') { valores[0].textContent=r.custo; card.querySelector('.result-state').textContent='Rota confirmada · nave em trânsito'; }
    card.querySelector('.progress-fill').style.width=(100*s.cursor/r.eventos.length)+'%';
    finalizar(i);
  });
  verificarConclusao();
}
function moverNaves(dt) {
  estados.forEach((s,i) => {
    const m=s.movimento;
    if(!m) return;
    m.tempo+=dt;
    const k=Math.min(1,m.tempo/m.duracao);
    s.nave=[m.de[0]+(m.para[0]-m.de[0])*k,m.de[1]+(m.para[1]-m.de[1])*k];
    const delta=Math.atan2(Math.sin(m.angulo-s.angulo),Math.cos(m.angulo-s.angulo));
    s.angulo+=delta*Math.min(1,dt/55);
    if(k===1) { s.nave=[...m.para]; s.movimento=null; finalizar(i); }
  });
  verificarConclusao();
}
function centralizar() { camera.zoom=1; camera.x=0; camera.y=0; $('zoomLabel').textContent='100%'; }
function zoom(fator,x=largura/2,y=altura/2) {
  const anterior=camera.zoom;
  camera.zoom=Math.min(3.5,Math.max(.7,camera.zoom*fator));
  const k=camera.zoom/anterior;
  camera.x=x-largura/2-(x-largura/2-camera.x)*k;
  camera.y=y-altura/2-(y-altura/2-camera.y)*k;
  limitarCamera(); $('zoomLabel').textContent=Math.round(camera.zoom*100)+'%';
}
function limitarCamera() {
  camera.x=Math.max(-largura*camera.zoom*.65,Math.min(largura*camera.zoom*.65,camera.x));
  camera.y=Math.max(-altura*camera.zoom*.65,Math.min(altura*camera.zoom*.65,camera.y));
}
function circulo(x,y,r,cor) { ctx.beginPath();ctx.arc(x,y,Math.max(0,r),0,TAU);ctx.fillStyle=cor;ctx.fill(); }
function halo(x,y,r,cor) {
  const g=ctx.createRadialGradient(x,y,0,x,y,r);g.addColorStop(0,cor);g.addColorStop(1,'transparent');circulo(x,y,r,g);
}
function estrela(x,y,r,cor) {
  ctx.fillStyle=cor;ctx.beginPath();ctx.moveTo(x,y-r*3);ctx.quadraticCurveTo(x+r*.3,y-r*.3,x+r*3,y);ctx.quadraticCurveTo(x+r*.3,y+r*.3,x,y+r*3);ctx.quadraticCurveTo(x-r*.3,y+r*.3,x-r*3,y);ctx.quadraticCurveTo(x-r*.3,y-r*.3,x,y-r*3);ctx.fill();
}
function criarFundo(w,h) {
  fundo=document.createElement('canvas');fundo.width=Math.ceil(w);fundo.height=Math.ceil(h);
  const f=fundo.getContext('2d');f.fillStyle='#030713';f.fillRect(0,0,w,h);
  [[.75,.28,.55,'#38315b',.28],[.35,.63,.55,'#123a4c',.32],[.95,.85,.4,'#472b49',.15]].forEach(([x,y,r,cor,a]) => {
    const g=f.createRadialGradient(w*x,h*y,0,w*x,h*y,w*r);g.addColorStop(0,cor);g.addColorStop(1,'transparent');f.globalAlpha=a;f.fillStyle=g;f.fillRect(0,0,w,h);
  });
  // Nebulosa determinística em cache para evitar trabalho extra por quadro.
  for(let j=0;j<85;j++) {
    const x=aleatorio(j+1)*w,y=h*(.18+.6*x/w)+(aleatorio(j+200)-.5)*h*.45,r=20+aleatorio(j+400)*w*.16;
    const g=f.createRadialGradient(x,y,0,x,y,r);g.addColorStop(0,j%2?'#6599bd':'#987abb');g.addColorStop(1,'transparent');f.globalAlpha=.015+aleatorio(j+500)*.025;f.fillStyle=g;f.fillRect(x-r,y-r,r*2,r*2);
  }
  f.globalAlpha=1;
  for(let j=0;j<650;j++) {
    const x=aleatorio(j+800)*w,y=aleatorio(j+1800)*h;
    f.globalAlpha=.15+aleatorio(j+2800)*.65;f.fillStyle=j%9===0?'#ead8b1':j%5===0?'#9fc8f9':'#d9e4f4';f.beginPath();f.arc(x,y,j%23===0?1.1:.25+aleatorio(j+3800)*.5,0,TAU);f.fill();
  }
  f.globalAlpha=1;fundoSujo=false;
}
function rocha(o,s,t) {
  const [x,y]=centro(o.p), seed=o.seed;
  const movimento=motionPreference.matches?0:Math.sin(t*.00022+seed)*s*.035;
  ctx.save();ctx.translate(x+movimento,y+movimento*.6);ctx.rotate(seed+t*.000035*(seed%2?1:-1));
  // Fragmentos ficam dentro do setor bloqueado, preservando a leitura da colisão.
  const g=ctx.createLinearGradient(-s*.3,-s*.3,s*.3,s*.3);g.addColorStop(0,'#a6a2a0');g.addColorStop(.35,'#686b74');g.addColorStop(1,'#242d3e');
  ctx.fillStyle=g;ctx.strokeStyle='#b8bbc72b';ctx.lineWidth=.7;ctx.beginPath();
  for(let j=0;j<9;j++) { const a=j*TAU/9,r=s*(.26+aleatorio(seed+j)*.13);if(j===0)ctx.moveTo(Math.cos(a)*r,Math.sin(a)*r);else ctx.lineTo(Math.cos(a)*r,Math.sin(a)*r); }
  ctx.closePath();ctx.fill();ctx.stroke();
  circulo(-s*.08,-s*.08,s*.085,'#20293688');circulo(s*.13,s*.045,s*.05,'#20293699');
  ctx.strokeStyle='#b4b6bc44';ctx.beginPath();ctx.arc(-s*.08,-s*.08,s*.085,Math.PI,TAU);ctx.stroke();
  for(let j=0;j<3;j++) {const a=seed+j*2.2;circulo(Math.cos(a)*s*.43,Math.sin(a)*s*.43,s*.025,'#8c8b9166');}
  ctx.restore();
}
function buracoNegro(o,s,t) {
  const [x,y]=centro(o.p),r=s*o.r;
  ctx.save();ctx.translate(x,y);
  halo(0,0,r*1.65,'#8961c82d');
  ctx.rotate(-.35);
  for(let j=0;j<5;j++) {
    ctx.strokeStyle=['#65417a33','#a178ba55','#f2d3ba99','#d2b5f9aa','#8061a744'][j];
    ctx.lineWidth=r*(j===2?.075:.035);ctx.beginPath();ctx.ellipse(0,0,r*(1.02+j*.07),r*(.4+j*.025),0,0,TAU);ctx.stroke();
  }
  const g=ctx.createRadialGradient(0,0,r*.48,0,0,r*.77);g.addColorStop(0,'#02030a');g.addColorStop(.73,'#04030b');g.addColorStop(.94,'#a986d2');g.addColorStop(1,'#dab1eb');
  ctx.shadowColor='#b48de8';ctx.shadowBlur=r*.3;circulo(0,0,r*.72,g);ctx.shadowBlur=0;
  ctx.strokeStyle='#efcdadbb';ctx.lineWidth=r*.04;ctx.beginPath();ctx.ellipse(0,0,r*1.18,r*.44,0,0,Math.PI);ctx.stroke();
  for(let j=0;j<9;j++) {
    const a=t*.00055+j*TAU/9+o.seed;
    circulo(Math.cos(a)*r*1.22,Math.sin(a)*r*.46,r*.02,j%2?'#d8b8f0':'#f4dfbe');
  }
  ctx.restore();
  if(o.r>1) textoCarta('SINGULARIDADE '+(o.p[0]===6?'01':'02'),x,y+r*1.7,'#b99bcf',7);
}
function textoCarta(texto,x,y,cor,tamanho=9) {
  ctx.save();ctx.font=`${tamanho}px 'Segoe UI',Arial,sans-serif`;ctx.textAlign='center';ctx.fillStyle=cor;ctx.shadowColor='#030713';ctx.shadowBlur=5;ctx.fillText(texto,x,y);ctx.restore();
}
function planeta(p,i,s,t) {
  const [x,y]=centro(p.p),r=s*.59;
  const selecionado=$('target').value===p.p.join(',');
  halo(x,y,r*2.1,p.cor+'20');
  ctx.save();ctx.translate(x,y);
  ctx.strokeStyle=p.cor+(selecionado?'99':'33');ctx.lineWidth=.7;ctx.beginPath();ctx.ellipse(0,0,r*1.55,r*1.03,-.4,0,TAU);ctx.stroke();
  const a=t*.00012+i*2;circulo(Math.cos(a)*r*1.55,Math.sin(a)*r*1.03,1.1,p.cor);
  const g=ctx.createRadialGradient(-r*.4,-r*.4,0,0,0,r);g.addColorStop(0,p.cor);g.addColorStop(.45,p.secundaria);g.addColorStop(1,'#0b1525');
  ctx.shadowColor=p.cor+'88';ctx.shadowBlur=r*.35;circulo(0,0,r,g);ctx.shadowBlur=0;
  ctx.save();ctx.beginPath();ctx.arc(0,0,r,0,TAU);ctx.clip();
  for(let j=0;j<5;j++) {
    ctx.strokeStyle=p.cor+(j%2?'33':'18');ctx.lineWidth=r*.14;ctx.beginPath();ctx.ellipse(-r*.15,(j-2)*r*.39,r*1.1,r*.16,-.3,0,TAU);ctx.stroke();
  }
  const nuit=ctx.createLinearGradient(-r,0,r,0);nuit.addColorStop(0,'transparent');nuit.addColorStop(.4,'#02061511');nuit.addColorStop(1,'#020615cc');ctx.fillStyle=nuit;ctx.fillRect(-r,-r,r*2,r*2);ctx.restore();
  ctx.strokeStyle=p.cor+'88';ctx.lineWidth=.8;ctx.beginPath();ctx.arc(0,0,r,-Math.PI,Math.PI*.1);ctx.stroke();
  if(selecionado || hover?.planeta===p) {
    ctx.strokeStyle=p.cor+'bb';ctx.lineWidth=1;ctx.setLineDash([3,5]);ctx.beginPath();ctx.arc(0,0,r*1.9,0,TAU);ctx.stroke();ctx.setLineDash([]);
  }
  ctx.restore();
  textoCarta(p.nome,x,y+r*2.35,'#e1e8f0',Math.max(8,Math.min(10,s*.42)));
  if(s>17) textoCarta(p.sistema,x,y+r*2.35+11,'#7e8fa9',6.5);
}
function nave(x,y,s,angulo,voando,t,cor) {
  const tamanho=Math.max(10,s*.67);
  ctx.save();ctx.translate(x,y);ctx.rotate(angulo);ctx.scale(tamanho/24,tamanho/24);
  // Silhueta de exploradora: disco principal, casco e duas naceles paralelas.
  if(voando) {
    for(const lado of [-1,1]) {
      const chama=30+(Math.sin(t*.022)+1)*6;
      const g=ctx.createLinearGradient(lado*15,15,lado*15,chama);g.addColorStop(0,'#c4f6ffdd');g.addColorStop(.2,cor+'bb');g.addColorStop(1,'transparent');
      ctx.fillStyle=g;ctx.beginPath();ctx.moveTo(lado*15-2,14);ctx.lineTo(lado*15, chama);ctx.lineTo(lado*15+2,14);ctx.fill();
    }
  }
  ctx.shadowColor=cor;ctx.shadowBlur=7;ctx.strokeStyle='#9aafc7';ctx.lineWidth=3;ctx.beginPath();ctx.moveTo(-3,4);ctx.lineTo(-15,11);ctx.moveTo(3,4);ctx.lineTo(15,11);ctx.stroke();ctx.shadowBlur=0;
  ctx.fillStyle='#a7bfd2';ctx.beginPath();ctx.ellipse(0,7,4,12,0,0,TAU);ctx.fill();
  for(const lado of [-1,1]) {
    ctx.fillStyle='#c4d8e5';ctx.beginPath();ctx.roundRect(lado*15-2.3,-2,4.6,23,2);ctx.fill();
    ctx.fillStyle='#8bd5e6';ctx.fillRect(lado*15-1,-.5,2,12);
    circulo(lado*15,18,1.8,voando?cor:'#517e95');circulo(lado*15,-1,1.6,'#e39d81');
  }
  const g=ctx.createRadialGradient(-4,-10,1,0,-6,14);g.addColorStop(0,'#f1f4ef');g.addColorStop(.6,'#c0d0db');g.addColorStop(1,'#64849d');
  ctx.fillStyle=g;ctx.beginPath();ctx.ellipse(0,-7,13.5,11.5,0,0,TAU);ctx.fill();
  ctx.strokeStyle='#6b8da4';ctx.lineWidth=.7;ctx.beginPath();ctx.ellipse(0,-7,10.5,8.5,0,0,TAU);ctx.stroke();circulo(0,-7,3,'#6e9db3');circulo(0,-8,1.5,'#d5eaf0');
  ctx.strokeStyle='#738f9e';ctx.lineWidth=.6;ctx.beginPath();ctx.moveTo(-9,-6);ctx.lineTo(-4,-6);ctx.moveTo(4,-6);ctx.lineTo(9,-6);ctx.stroke();
  ctx.restore();
}
function centro(p) { return [geometria.ox+(p[1]+.5)*geometria.s,geometria.oy+(p[0]+.5)*geometria.s]; }
function desenhar(t) {
  const rect=canvas.getBoundingClientRect(),dpr=Math.min(devicePixelRatio||1,2);
  largura=rect.width;altura=rect.height;
  if(canvas.width!==Math.round(largura*dpr)||canvas.height!==Math.round(altura*dpr)) {
    canvas.width=Math.round(largura*dpr);canvas.height=Math.round(altura*dpr);fundoSujo=true;
  }
  ctx.setTransform(dpr,0,0,dpr,0,0);
  if(fundoSujo) criarFundo(largura,altura);
  ctx.drawImage(fundo,0,0,largura,altura);
  // Paralaxe discreta e cintilação sem mover os setores de navegação.
  for(let j=0;j<18;j++) {
    const x=(aleatorio(j+9000)*largura+t*.00045*(1+j%3))%largura,y=aleatorio(j+9100)*altura;
    const a=.18+(.5+.5*Math.sin(t*.0007+j))*.4;
    ctx.globalAlpha=a;estrela(x+camera.x*.025,y+camera.y*.025,j%3===0?1.2:.65,'#c9def5');
  }
  ctx.globalAlpha=1;
  if(!mapa) return;
  const linhas=mapa.linhas.length,colunas=mapa.linhas[0].length;
  const s=Math.min(48,(largura-68)/colunas,(altura-175)/linhas);
  geometria={s,ox:(largura-colunas*s)/2,oy:55+(altura-175-linhas*s)/2};
  const e=estados[ativo],cor=cores[ativo];
  ctx.save();ctx.translate(largura/2+camera.x,altura/2+camera.y);ctx.scale(camera.zoom,camera.zoom);ctx.translate(-largura/2,-altura/2);
  // Referências orbitais suaves reforçam a escala sem poluir os corredores.
  const [sx,sy]=centro(mapa.inicio);
  ctx.strokeStyle='#5f8ca314';ctx.lineWidth=.7;ctx.setLineDash([2,7]);
  for(const raio of [s*3.1,s*7.5,s*13]) {ctx.beginPath();ctx.arc(sx,sy,raio,0,TAU);ctx.stroke();}ctx.setLineDash([]);
  if($('grid').checked) {
    ctx.strokeStyle='#89a7c21a';ctx.lineWidth=.6;ctx.beginPath();
    for(let l=0;l<=linhas;l++){ctx.moveTo(geometria.ox,geometria.oy+l*s);ctx.lineTo(geometria.ox+colunas*s,geometria.oy+l*s);}
    for(let c=0;c<=colunas;c++){ctx.moveTo(geometria.ox+c*s,geometria.oy);ctx.lineTo(geometria.ox+c*s,geometria.oy+linhas*s);}ctx.stroke();
    // Mostra com exatidão os setores bloqueados, inclusive ao redor dos buracos negros.
    ctx.fillStyle='#a9a0ba17';mapa.linhas.forEach((l,r)=>{for(let c=0;c<colunas;c++)if(l[c]==='1')ctx.fillRect(geometria.ox+c*s,geometria.oy+r*s,s,s);});
  }
  e.celulas.forEach((tipo,chave) => {
    const [l,c]=chave.split(',').map(Number),[x,y]=centro([l,c]);
    ctx.fillStyle=tipo==='CAMINHO'?cor+'12':tipo==='PROCESSADA'?cor+'13':'#e5bf7a10';
    ctx.beginPath();ctx.roundRect(x-s*.45,y-s*.45,s*.9,s*.9,s*.12);ctx.fill();
    if(tipo!=='CAMINHO') circulo(x,y,tipo==='PROCESSADA'?Math.max(.7,s*.045):s*.065,tipo==='PROCESSADA'?cor+'70':'#e5bf7aaa');
  });
  if(resultados[ativo] && !e.rota.length && !e.finalizado && rodando) {
    const rayon=(t*.012)%(s*14);
    ctx.strokeStyle=cor+'18';ctx.lineWidth=1;ctx.beginPath();ctx.arc(sx,sy,rayon,0,TAU);ctx.stroke();
  }
  desenharObjetos(s,t);
  planetas.forEach((p,i)=>planeta(p,i,s,t));
  if(e.rota.length) {
    ctx.strokeStyle=cor;ctx.lineWidth=Math.max(1.5,s*.065);ctx.lineJoin='round';ctx.lineCap='round';ctx.shadowColor=cor;ctx.shadowBlur=9;ctx.beginPath();
    e.rota.forEach((p,i)=>{const [x,y]=centro(e.movimento && i===e.rota.length-1?e.nave:p);if(i===0)ctx.moveTo(x,y);else ctx.lineTo(x,y);});ctx.stroke();ctx.shadowBlur=0;
    e.rota.filter((_,i)=>i%3===0).forEach(p=>{const [x,y]=centro(p);circulo(x,y,Math.max(1,s*.055),cor);});
  }
  if(e.atual && e.atual.tipo!=='CAMINHO' && !e.finalizado) {
    const [x,y]=centro(e.atual.posicao);
    ctx.strokeStyle='#f1d396aa';ctx.lineWidth=1;ctx.strokeRect(x-s*.4,y-s*.4,s*.8,s*.8);
  }
  // A origem permanece marcada durante o voo.
  if(e.rota.length) {ctx.strokeStyle=cor+'77';ctx.lineWidth=.7;ctx.beginPath();ctx.arc(sx,sy,s*.36,0,TAU);ctx.stroke();}
  if(cenario==='solar' && !e.rota.length) textoCarta('SOL · PONTO DE PARTIDA',sx,sy+s*1.45,'#7897ac',7);
  const [nx,ny]=centro(e.nave);
  halo(nx,ny,s*1.2,cor+'12');
  if(e.finalizado && resultados[ativo]?.encontrou) {
    const debut=e.chegada??t;
    for(let j=0;j<2;j++) {
      const k=((t-debut)*.0003+j*.5)%1;
      ctx.globalAlpha=(1-k)*.5;ctx.strokeStyle=cor;ctx.lineWidth=1;ctx.beginPath();ctx.arc(nx,ny,s*(.7+k*1.7),0,TAU);ctx.stroke();
    }ctx.globalAlpha=1;
  }
  nave(nx,ny,s,e.angulo,!!e.movimento&&rodando,t,cor);
  if(hover && !arraste) {
    const [hx,hy]=centro(hover.p);ctx.strokeStyle='#c6d8eb44';ctx.lineWidth=.8;ctx.strokeRect(hx-s*.5,hy-s*.5,s,s);
  }
  ctx.restore();
}
function desenharObjetos(s,t) { objetos.forEach(o=>o.tipo==='rocha'?rocha(o,s,t):buracoNegro(o,s,t)); }
function frame(t) {
  const dt=ultimoFrame?Math.min(50,t-ultimoFrame):0;ultimoFrame=t;
  if(!document.hidden) {
    if($('ambient').checked && !motionPreference.matches) tempoAmbiente+=dt;
    if(rodando) {
      moverNaves(dt);acumulado+=dt;
      const intervalo=Number($('speed').value);
      let passos=0;
      while(rodando && acumulado>=intervalo && passos++<8) {avancar();acumulado-=intervalo;}
    }
    desenhar(tempoAmbiente);
  }
  requestAnimationFrame(frame);
}
function posicaoPonteiro(evento) {
  if(!geometria) return null;
  const rect=canvas.getBoundingClientRect();
  const x=evento.clientX-rect.left,y=evento.clientY-rect.top;
  const wx=(x-largura/2-camera.x)/camera.zoom+largura/2,wy=(y-altura/2-camera.y)/camera.zoom+altura/2;
  const l=Math.floor((wy-geometria.oy)/geometria.s),c=Math.floor((wx-geometria.ox)/geometria.s);
  return {x,y,p:[l,c],valido:l>=0&&c>=0&&l<mapa.linhas.length&&c<mapa.linhas[0].length,wx,wy};
}
function atualizarHover(evento) {
  const pos=posicaoPonteiro(evento);
  if(!pos?.valido || arraste) {hover=null;$('tooltip').hidden=true;return;}
  const planeta=planetas.find(p=>Math.hypot(...centro(p.p).map((v,i)=>v-[pos.wx,pos.wy][i]))<geometria.s*.9);
  hover={...pos,planeta};
  const obstaculo=objetos.find(o=>o.tipo==='buraco' && Math.abs(o.p[0]-pos.p[0])<=Math.floor(o.r) && Math.abs(o.p[1]-pos.p[1])<=Math.floor(o.r));
  const bloqueado=mapa.linhas[pos.p[0]][pos.p[1]]==='1';
  const titulo=planeta?planeta.nome+'\n'+planeta.sistema+' · clique para selecionar':obstaculo?'Buraco negro · setor bloqueado':bloqueado?'Campo de asteroides · setor bloqueado':igual(pos.p,mapa.inicio)?'Exploradora NCC–01 · origem':null;
  canvas.style.cursor=planeta&&!ocupado?'pointer':'grab';
  if(titulo) {
    const tt=$('tooltip');tt.textContent=titulo;tt.hidden=false;
    tt.style.left=Math.max(8,Math.min(largura-tt.offsetWidth-10,pos.x+15))+'px';
    tt.style.top=Math.max(42,Math.min(altura-tt.offsetHeight-12,pos.y+15))+'px';
  } else $('tooltip').hidden=true;
}
$('launch').onclick=iniciar;
$('pause').onclick=()=>{rodando=!rodando;acumulado=0;botoes();atualizarHud();anunciar(rodando?'Missão retomada. Scanner e propulsão ativos.':'Missão pausada. Use Um passo para inspecionar cada evento.');};
$('step').onclick=()=>{rodando=false;avancar(true);};
$('reset').onclick=()=>{limpar();anunciar('Missão reiniciada. Nave de volta ao ponto de partida.');};
$('target').onchange=()=>{limpar();anunciar('Destino atualizado. Inicie a exploração para traçar uma rota segura.');};
$('algorithm').onchange=()=>{limpar();if($('algorithm').value!=='ambos')selecionar(Number($('algorithm').value));anunciar('Sistema de navegação atualizado.');};
[0,1].forEach(i=>$('tab'+i).onclick=()=>selecionar(i));
$('speed').oninput=()=>$('speedLabel').textContent=$('speed').value+' ms / evento';
$('zoomIn').onclick=()=>zoom(1.2);$('zoomOut').onclick=()=>zoom(1/1.2);$('fit').onclick=centralizar;
$('load').onclick=()=>$('file').click();
$('scenario').onchange=()=>{
  if($('scenario').value==='solar') {cenario='solar';const m=criarCenario();usarMapa(m,m.linhas.join('\n'),'Carta da fronteira solar');}
  else if($('scenario').value==='original' && original) {cenario='original';usarMapa(original,original.linhas.join('\n'),'Mapa original do projeto');}
  else if(importado) {cenario='importado';usarMapa(importado.mapa,importado.texto,importado.nome);}
};
$('file').onchange=async()=>{
  const f=$('file').files[0];if(!f)return;
  const estavaRodando=rodando;ocupado=true;rodando=false;botoes();atualizarHud();
  try {
    if(f.size>65536)throw new Error('O mapa deve ter até 64 KB.');
    const texto=await f.text();
    const dados=await resposta(fetch('/api/buscar',{method:'POST',body:texto}));
    cenario='importado';importado={mapa:dados.mapa,texto,nome:f.name};
    if(!$('scenario').querySelector('option[value="importado"]')) $('scenario').add(new Option('Mapa importado','importado'));
    $('scenario').value='importado';usarMapa(dados.mapa,texto,f.name);
  } catch(e) {rodando=estavaRodando;anunciar('Não foi possível carregar: '+e.message);}
  finally {ocupado=false;botoes();atualizarHud();$('file').value='';}
};
canvas.addEventListener('wheel',e=>{e.preventDefault();const r=canvas.getBoundingClientRect();zoom(Math.exp(-e.deltaY*.001),e.clientX-r.left,e.clientY-r.top);hover=null;$('tooltip').hidden=true;},{passive:false});
canvas.addEventListener('pointerdown',e=>{
  if(e.button!==0 || arraste) return;
  const pos=posicaoPonteiro(e);
  arraste={id:e.pointerId,x:e.clientX,y:e.clientY,cx:camera.x,cy:camera.y,movido:false,planeta:pos&&planetas.find(p=>Math.hypot(...centro(p.p).map((v,i)=>v-[pos.wx,pos.wy][i]))<geometria.s*.9)};
  canvas.setPointerCapture(e.pointerId);canvas.classList.add('dragging');$('tooltip').hidden=true;
});
canvas.addEventListener('pointermove',e=>{
  if(arraste && arraste.id===e.pointerId) {
    const dx=e.clientX-arraste.x,dy=e.clientY-arraste.y;
    if(Math.hypot(dx,dy)>4) arraste.movido=true;
    if(arraste.movido) {camera.x=arraste.cx+dx;camera.y=arraste.cy+dy;limitarCamera();}
  } else atualizarHover(e);
});
function terminarArraste(e,cancelado=false) {
  if(!arraste || arraste.id!==e.pointerId) return;
  const clicado=!cancelado&&!arraste.movido&&arraste.planeta;
  arraste=null;canvas.classList.remove('dragging');
  if(canvas.hasPointerCapture(e.pointerId)) canvas.releasePointerCapture(e.pointerId);
  if(clicado&&!ocupado) {$('target').value=clicado.p.join(',');$('target').onchange();}
  atualizarHover(e);
}
canvas.addEventListener('pointerup',e=>terminarArraste(e));
canvas.addEventListener('pointercancel',e=>terminarArraste(e,true));
canvas.addEventListener('lostpointercapture',()=>{arraste=null;canvas.classList.remove('dragging');});
canvas.addEventListener('pointerleave',()=>{hover=null;$('tooltip').hidden=true;canvas.style.cursor='grab';});
canvas.addEventListener('keydown',e=>{
  if(['+','=','-','0','ArrowLeft','ArrowRight','ArrowUp','ArrowDown'].includes(e.key)) e.preventDefault();
  if(e.key==='+'||e.key==='=')zoom(1.2);
  if(e.key==='-')zoom(1/1.2);
  if(e.key==='0')centralizar();
  if(e.key==='ArrowLeft')camera.x+=35;
  if(e.key==='ArrowRight')camera.x-=35;
  if(e.key==='ArrowUp')camera.y+=35;
  if(e.key==='ArrowDown')camera.y-=35;
  limitarCamera();
});
motionPreference.addEventListener('change',e=>{if(e.matches)$('ambient').checked=false;});
if(motionPreference.matches)$('ambient').checked=false;
limpar();
const solar=criarCenario();usarMapa(solar,solar.linhas.join('\n'),'Carta da fronteira solar');
const opcaoOriginal=$('scenario').querySelector('[value="original"]');opcaoOriginal.disabled=true;
$('launch').disabled=true;ocupado=true;botoes();
resposta(fetch('/api/mapa')).then(m=>{original=m;opcaoOriginal.disabled=false;anunciar('Central online. A fronteira solar está pronta para exploração.');})
  .catch(e=>anunciar('Central Java indisponível: '+e.message+'. Inicie o servidor para executar as buscas.'))
  .finally(()=>{ocupado=false;botoes();atualizarHud();});
requestAnimationFrame(frame);
