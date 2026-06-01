/* shared.jsx — icons, navigation, common bits. Loaded via Babel. */
const { useState, useEffect, useRef, useMemo } = React;

/* ---------- Iconography: hand-drawn 1.6px stroke, rounded, no fill ---------- */
const Ic = ({ d, size = 20, sw = 1.6, fill, stroke = "currentColor", style }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={stroke}
       strokeWidth={sw} strokeLinecap="round" strokeLinejoin="round" style={style}>
    {Array.isArray(d) ? d.map((p,i)=><path key={i} d={p} fill={fill||"none"}/>) : <path d={d} fill={fill||"none"}/>}
  </svg>
);

const Icon = {
  home:     (p)=><Ic {...p} d="M3 11.5L12 4l9 7.5M5 10.5V20h14V10.5"/>,
  cart:     (p)=><Ic {...p} d={["M3 4h2l2.5 12h11L21 7H7","M9 20.5a1.2 1.2 0 1 0 0-2.4 1.2 1.2 0 0 0 0 2.4z","M17 20.5a1.2 1.2 0 1 0 0-2.4 1.2 1.2 0 0 0 0 2.4z"]}/>,
  chart:    (p)=><Ic {...p} d={["M4 20V10","M10 20V4","M16 20v-7","M22 20H2"]}/>,
  report:   (p)=><Ic {...p} d={["M6 3h9l5 5v13H6z","M14 3v6h6","M9 14h6M9 17h4"]}/>,
  box:      (p)=><Ic {...p} d={["M3 7l9-4 9 4-9 4-9-4z","M3 7v10l9 4 9-4V7","M12 11v10"]}/>,
  people:   (p)=><Ic {...p} d={["M9 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7z","M2 21c0-3.5 3-6 7-6s7 2.5 7 6","M17 11a3 3 0 1 0 0-6","M16 21h6c0-2.5-1.8-4.7-4.5-5.4"]}/>,
  truck:    (p)=><Ic {...p} d={["M3 6h11v9H3z","M14 9h4l3 3v3h-7","M7 18.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z","M17 18.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z"]}/>,
  wallet:   (p)=><Ic {...p} d={["M3 7h16a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7z","M3 7V6a2 2 0 0 1 2-2h12","M16 13h3"]}/>,
  setting:  (p)=><Ic {...p} d={["M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z","M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1 1.7 1.7 0 0 0-.3-1.8L4.2 7a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"]}/>,
  search:   (p)=><Ic {...p} d={["M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14z","M21 21l-4.3-4.3"]}/>,
  barcode:  (p)=><Ic {...p} d={["M4 6v12","M7 6v12","M10 6v12","M13 6v12","M16 6v12","M19 6v12"]}/>,
  plus:     (p)=><Ic {...p} d={["M12 5v14","M5 12h14"]}/>,
  minus:    (p)=><Ic {...p} d={["M5 12h14"]}/>,
  close:    (p)=><Ic {...p} d={["M6 6l12 12","M6 18L18 6"]}/>,
  check:    (p)=><Ic {...p} d={["M5 12l5 5L20 7"]}/>,
  arrow_r:  (p)=><Ic {...p} d={["M5 12h14","M13 6l6 6-6 6"]}/>,
  chev_r:   (p)=><Ic {...p} d={["M9 6l6 6-6 6"]}/>,
  chev_d:   (p)=><Ic {...p} d={["M6 9l6 6 6-6"]}/>,
  bell:     (p)=><Ic {...p} d={["M6 16V11a6 6 0 1 1 12 0v5l2 2H4z","M10 21a2 2 0 1 0 4 0"]}/>,
  user:     (p)=><Ic {...p} d={["M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8z","M4 21c0-4 4-7 8-7s8 3 8 7"]}/>,
  trash:    (p)=><Ic {...p} d={["M4 7h16","M9 7V4h6v3","M6 7l1 13h10l1-13"]}/>,
  print:    (p)=><Ic {...p} d={["M7 8V3h10v5","M5 8h14a2 2 0 0 1 2 2v6h-4v4H7v-4H3v-6a2 2 0 0 1 2-2z"]}/>,
  share:    (p)=><Ic {...p} d={["M4 12v7a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-7","M12 3v13","M7 8l5-5 5 5"]}/>,
  filter:   (p)=><Ic {...p} d={["M3 5h18","M6 12h12","M10 19h4"]}/>,
  download: (p)=><Ic {...p} d={["M12 3v13","M7 12l5 5 5-5","M5 21h14"]}/>,
  star:     (p)=><Ic {...p} d="M12 3l2.7 6 6.3.6-4.8 4.4 1.4 6.4L12 17l-5.6 3.4L7.8 14 3 9.6 9.3 9z"/>,
  sun:      (p)=><Ic {...p} d={["M12 4v2","M12 18v2","M4 12H2","M22 12h-2","M5 5l1.5 1.5","M17.5 17.5L19 19","M5 19l1.5-1.5","M17.5 6.5L19 5","M12 16a4 4 0 1 0 0-8 4 4 0 0 0 0 8z"]}/>,
  moon:     (p)=><Ic {...p} d="M20 14.5A8 8 0 0 1 9.5 4 8 8 0 1 0 20 14.5z"/>,
  scan:     (p)=><Ic {...p} d={["M4 7V5a1 1 0 0 1 1-1h2","M17 4h2a1 1 0 0 1 1 1v2","M20 17v2a1 1 0 0 1-1 1h-2","M7 20H5a1 1 0 0 1-1-1v-2","M4 12h16"]}/>,
  card:     (p)=><Ic {...p} d={["M3 7h18v11H3z","M3 11h18","M7 15h3"]}/>,
  cash:     (p)=><Ic {...p} d={["M2 7h20v10H2z","M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z","M5 9h2M17 15h2"]}/>,
  mobile:   (p)=><Ic {...p} d={["M7 3h10v18H7z","M11 18h2"]}/>,
  receipt:  (p)=><Ic {...p} d={["M6 3h12v18l-3-2-3 2-3-2-3 2z","M9 8h6M9 12h6M9 16h4"]}/>,
  arrowup:  (p)=><Ic {...p} d={["M7 14l5-5 5 5"]}/>,
  arrowdn:  (p)=><Ic {...p} d={["M7 10l5 5 5-5"]}/>,
  more:     (p)=><Ic {...p} d={["M5 12h.01","M12 12h.01","M19 12h.01"]} sw={3}/>,
  menu:     (p)=><Ic {...p} d={["M4 6h16","M4 12h16","M4 18h16"]}/>,
  refresh:  (p)=><Ic {...p} d={["M21 12a9 9 0 1 1-3-6.7L21 8","M21 3v5h-5"]}/>,
  logo:     ({size=28})=>(
    <svg width={size} height={size} viewBox="0 0 32 32" fill="none">
      <rect x="2" y="2" width="28" height="28" rx="7" fill="#14110C"/>
      <path d="M9 22V10l7 8V10" stroke="#E8651D" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"/>
      <path d="M19 14h4M19 18h4M19 22h2" stroke="#F4ECDD" strokeWidth="1.6" strokeLinecap="round"/>
    </svg>
  ),
};

/* ---------- Status bar (Android tablet, landscape) ---------- */
function StatusBar({ time="14:08", net="LTE", theme="light", onToggle }) {
  return (
    <div className="statusbar">
      <div style={{display:"flex",gap:10,alignItems:"center"}}>
        {/* tiny notification dots (Android) */}
        <span style={{display:"inline-flex",alignItems:"center",gap:5}}>
          <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor"><path d="M12 22a2 2 0 0 0 2-2h-4a2 2 0 0 0 2 2zm7-6V11a7 7 0 0 0-5-6.7V4a2 2 0 1 0-4 0v.3A7 7 0 0 0 5 11v5l-2 2h18z"/></svg>
          <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor"><path d="M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z" opacity=".55"/></svg>
        </span>
        <span style={{fontWeight:600}}>{time}</span>
      </div>
      <div className="right">
        {/* signal bars */}
        <span style={{display:"inline-flex",alignItems:"flex-end",gap:1.5,height:12}}>
          {[3,5,8,11].map((h,i)=>(<span key={i} style={{width:2.5,height:h,background:"currentColor",opacity:i<3?.95:.5,borderRadius:0.5}}/>))}
        </span>
        <span style={{fontSize:10.5,fontFamily:"var(--mono)",letterSpacing:".05em",opacity:.8}}>{net}</span>
        {/* wifi arcs */}
        <svg width="13" height="12" viewBox="0 0 14 12" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"><path d="M1 4.2a8 8 0 0 1 12 0"/><path d="M3 6.4a5 5 0 0 1 8 0"/><path d="M5.2 8.6a2 2 0 0 1 3.6 0"/><circle cx="7" cy="10.5" r=".8" fill="currentColor"/></svg>
        {/* battery */}
        <span style={{display:"inline-flex",alignItems:"center",gap:4}}>
          <span className="num" style={{fontSize:11}}>87%</span>
          <span style={{display:"inline-block",width:22,height:10,border:"1.2px solid currentColor",borderRadius:2,position:"relative"}}>
            <span style={{position:"absolute",inset:1,right:"15%",background:"currentColor",borderRadius:1}}/>
            <span style={{position:"absolute",left:"100%",top:2,bottom:2,width:1.5,background:"currentColor",borderRadius:1}}/>
          </span>
        </span>
        {onToggle && (
          <button onClick={onToggle} title="Theme"
            style={{marginLeft:6,display:"inline-flex",alignItems:"center",gap:4,background:"transparent",border:"1px solid var(--hairline)",height:20,padding:"0 6px",borderRadius:99,fontSize:11,color:"var(--ink)",cursor:"pointer"}}>
            {theme==="light" ? <Icon.sun size={12}/> : <Icon.moon size={12}/>}
            <span style={{fontWeight:600}}>{theme==="light"?"Daylight":"Counter"}</span>
          </button>
        )}
      </div>
    </div>
  );
}

/* Android gesture nav pill (bottom of screen) */
function GestureBar() {
  return <div className="gesture-bar"/>;
}

/* ---------- Nav rail (tablet) ---------- */
function NavRail({ active="pos", onChange, business="QUINCAILLERIE\nRB TRADING" }) {
  /* If no explicit onChange, use global nav function set by prototype shell */
  const nav = onChange || window.__nexaNav || (()=>{});
  const items = [
    {id:"home",  label:"Home",      ic:"home"},
    {id:"pos",   label:"POS",       ic:"cart"},
    {id:"products", label:"Products", ic:"box"},
    {id:"parties", label:"Parties", ic:"people"},
    {id:"purchase",label:"Purchase", ic:"truck"},
    {id:"money",  label:"Money",    ic:"wallet"},
    {id:"reports",label:"Reports",  ic:"chart"},
    {id:"settings",label:"Settings",ic:"setting"},
  ];
  return (
    <div style={{
      width: 88, background: "var(--surface)", borderRight: "1px solid var(--hairline)",
      display:"flex", flexDirection:"column", alignItems:"center", padding: "14px 0 12px",
      backgroundImage:"var(--grain)", backgroundBlendMode:"multiply"
    }}>
      <div style={{marginBottom: 16, display:"flex",flexDirection:"column",alignItems:"center",gap:6}}>
        <Icon.logo size={36}/>
        <div style={{fontFamily:"var(--mono)",fontSize:9,letterSpacing:".14em",color:"var(--muted)",fontWeight:600}}>RB · 01</div>
      </div>
      <div style={{display:"flex",flexDirection:"column",gap:6,flex:1,width:"100%",alignItems:"center"}}>
        {items.map(it => {
          const on = active===it.id;
          const IcCmp = Icon[it.ic];
          return (
            <button key={it.id} onClick={()=>nav(it.id)}
              style={{
                width: 68, height: 56, border:0, cursor:"pointer",
                display:"flex", flexDirection:"column", alignItems:"center", justifyContent:"center", gap:3,
                borderRadius: 12,
                background: on ? "var(--ink)" : "transparent",
                color: on ? "var(--surface)" : "var(--graphite)",
                transition: "all .18s ease",
                position:"relative"
              }}>
              {on && <span style={{position:"absolute",left:-12,top:14,bottom:14,width:3,background:"var(--amber)",borderRadius:2}}/>}
              <IcCmp size={20}/>
              <span style={{fontSize:10.5,fontWeight:600,letterSpacing:".01em"}}>{it.label}</span>
            </button>
          );
        })}
      </div>
      <div style={{marginTop:8,display:"flex",flexDirection:"column",alignItems:"center",gap:8}}>
        <div style={{width:44,height:44,borderRadius:12,background:"var(--amber-soft)",color:"var(--amber-press)",display:"flex",alignItems:"center",justifyContent:"center",fontFamily:"var(--mono)",fontWeight:700}}>SK</div>
      </div>
    </div>
  );
}

/* ---------- App bar (tablet) ---------- */
function AppBar({ title, subtitle, right, business="QUINCAILLERIE RB TRADING" }) {
  return (
    <div style={{display:"flex",alignItems:"center",gap:18,padding:"14px 22px",borderBottom:"1px solid var(--hairline)",background:"var(--surface)"}}>
      <div style={{flex:1, minWidth:0}}>
        <div className="eyebrow" style={{marginBottom:4}}>{business} · Counter 01</div>
        <div style={{display:"flex",alignItems:"baseline",gap:12}}>
          <h1 style={{margin:0,fontSize:22,fontWeight:700,letterSpacing:"-0.015em"}}>{title}</h1>
          {subtitle && <span style={{color:"var(--muted)",fontSize:13}}>{subtitle}</span>}
        </div>
      </div>
      {right}
    </div>
  );
}

/* ---------- CountUp number ---------- */
function CountUp({ value, prefix="", duration=600, decimals=2 }) {
  const [v, setV] = useState(value);
  const prev = useRef(value);
  useEffect(()=>{
    const start = prev.current;
    const t0 = performance.now();
    let raf;
    const tick = (t)=>{
      const k = Math.min(1, (t - t0)/duration);
      const eased = 1 - Math.pow(1 - k, 3);
      setV(start + (value - start) * eased);
      if (k < 1) raf = requestAnimationFrame(tick);
      else prev.current = value;
    };
    raf = requestAnimationFrame(tick);
    return ()=>cancelAnimationFrame(raf);
  }, [value]);
  const s = v.toLocaleString("en-US",{minimumFractionDigits:decimals,maximumFractionDigits:decimals});
  return <span className="num">{prefix}{s}</span>;
}

/* ---------- Rs helper ---------- */
const Rs = (n, dec=2) => "Rs " + Number(n).toLocaleString("en-US",{minimumFractionDigits:dec,maximumFractionDigits:dec});
const RsK = (n) => "Rs " + (n>=1000 ? (n/1000).toFixed(2)+"K" : n.toFixed(0));

/* ---------- Product placeholder ----------
   Geometric, slightly textured tile representing the product. */
function ProductTile({ kind, size=80 }) {
  // kind: sprayer, drill, wrench, saw, scrubber, generic, paint, hammer, pipe
  const palettes = {
    sprayer:  ["#1F4D8A","#E8C341","#0E2E55"],
    drill:    ["#C7401A","#1A1714","#F4ECDD"],
    wrench:   ["#3A332A","#C7BAA0","#14110C"],
    saw:      ["#E8651D","#1A1714","#C7BAA0"],
    scrubber: ["#3F8B5E","#FBE5D2","#1F4D26"],
    paint:    ["#D03B3B","#F4ECDD","#1A1714"],
    hammer:   ["#7B3D14","#1A1714","#E8C341"],
    pipe:     ["#2A3A45","#C7BAA0","#1A1714"],
    generic:  ["#3B342A","#D9D1C2","#E8651D"],
  };
  const p = palettes[kind] || palettes.generic;
  return (
    <svg width={size} height={size} viewBox="0 0 80 80" style={{display:"block"}}>
      <defs>
        <linearGradient id={`g-${kind}`} x1="0" x2="0" y1="0" y2="1">
          <stop offset="0" stopColor={p[1]}/><stop offset="1" stopColor={p[0]} stopOpacity=".25"/>
        </linearGradient>
        <pattern id={`grid-${kind}`} width="8" height="8" patternUnits="userSpaceOnUse">
          <path d="M8 0H0V8" fill="none" stroke="rgba(0,0,0,0.05)" strokeWidth="1"/>
        </pattern>
      </defs>
      <rect width="80" height="80" fill={`url(#g-${kind})`}/>
      <rect width="80" height="80" fill={`url(#grid-${kind})`}/>
      {kind==="sprayer" && (<>
        <rect x="22" y="20" width="22" height="40" rx="3" fill={p[0]}/>
        <rect x="26" y="14" width="14" height="8" rx="2" fill={p[2]}/>
        <rect x="44" y="28" width="14" height="3" fill={p[1]}/>
        <circle cx="33" cy="32" r="3" fill={p[1]}/>
      </>)}
      {kind==="drill" && (<>
        <rect x="14" y="34" width="36" height="14" rx="3" fill={p[0]}/>
        <rect x="22" y="48" width="14" height="18" rx="3" fill={p[1]}/>
        <rect x="50" y="38" width="18" height="6" rx="1" fill={p[2]}/>
        <circle cx="68" cy="41" r="3" fill={p[0]}/>
      </>)}
      {kind==="wrench" && (<>
        <path d="M14 22 L40 48 L48 40 L22 14 Z" fill={p[0]}/>
        <circle cx="18" cy="18" r="6" fill="none" stroke={p[0]} strokeWidth="4"/>
        <circle cx="58" cy="58" r="6" fill="none" stroke={p[0]} strokeWidth="4"/>
      </>)}
      {kind==="saw" && (<>
        <path d="M10 50 L70 50 L66 56 L62 50 L58 56 L54 50 L50 56 L46 50 L42 56 L38 50 L34 56 L30 50 L26 56 L22 50 L18 56 L14 50 Z" fill={p[0]}/>
        <rect x="6" y="46" width="68" height="6" fill={p[1]}/>
        <rect x="2" y="38" width="14" height="14" rx="2" fill={p[2]}/>
      </>)}
      {kind==="scrubber" && (<>
        <rect x="14" y="20" width="52" height="16" rx="4" fill={p[0]}/>
        <rect x="16" y="36" width="48" height="20" rx="2" fill={p[1]}/>
        {Array.from({length:8}).map((_,i)=><line key={i} x1={20+i*5.5} y1={38} x2={20+i*5.5} y2={54} stroke={p[2]} strokeWidth="1.5"/>)}
      </>)}
      {kind==="paint" && (<>
        <rect x="22" y="20" width="36" height="44" rx="3" fill={p[0]}/>
        <rect x="20" y="16" width="40" height="6" rx="1" fill={p[2]}/>
        <rect x="34" y="6" width="6" height="14" fill={p[2]}/>
        <text x="40" y="46" fontSize="10" fontFamily="JetBrains Mono" fill={p[1]} textAnchor="middle" fontWeight="700">5L</text>
      </>)}
      {kind==="hammer" && (<>
        <rect x="20" y="14" width="40" height="14" rx="2" fill={p[1]}/>
        <rect x="24" y="28" width="6" height="44" fill={p[0]}/>
      </>)}
      {kind==="pipe" && (<>
        <rect x="10" y="32" width="60" height="16" fill={p[0]}/>
        <rect x="6" y="28" width="10" height="24" fill={p[1]}/>
        <rect x="64" y="28" width="10" height="24" fill={p[1]}/>
      </>)}
      {kind==="generic" && (<>
        <rect x="20" y="20" width="40" height="40" fill={p[0]} opacity=".9"/>
        <rect x="28" y="28" width="24" height="24" fill={p[2]}/>
      </>)}
    </svg>
  );
}

Object.assign(window, { Icon, Ic, StatusBar, GestureBar, NavRail, AppBar, CountUp, Rs, RsK, ProductTile });
