/* dashboard.jsx — Home / KPI dashboard (tablet) */
const { useState: udS, useEffect: udE, useMemo: udM } = React;

/* SVG path generator */
const linePath = (data, w, h, pad=8) => {
  const max = Math.max(...data, 1);
  const step = (w - pad*2) / (data.length-1);
  return data.map((v,i)=>{
    const x = pad + i*step;
    const y = h - pad - (v/max)*(h - pad*2);
    return (i===0?"M":"L") + x.toFixed(1)+","+y.toFixed(1);
  }).join(" ");
};

const SALES = [42,68,55,90,72,110,85, 78,95,120,140,118,135,150];
const PURCH = [30,55,40,72,60,80,65, 60,72,85,95,80,92,100];
const DAYS = ["13","14","15","16","17","18","19","20","21","22","23","24","25","26"];

function Spark({data, color="var(--amber)", w=120, h=40, area=true, dash}) {
  const pad = 4;
  const max = Math.max(...data,1);
  const step = (w - pad*2) / (data.length-1);
  const pts = data.map((v,i)=>[pad + i*step, h - pad - (v/max)*(h - pad*2)]);
  const d = pts.map((p,i)=>(i?"L":"M") + p[0].toFixed(1)+","+p[1].toFixed(1)).join(" ");
  const a = `${d} L ${pts.at(-1)[0]},${h-pad} L ${pts[0][0]},${h-pad} Z`;
  const gid = "sg-"+Math.random().toString(36).slice(2,8);
  return (
    <svg width={w} height={h} style={{overflow:"visible"}}>
      <defs><linearGradient id={gid} x1="0" x2="0" y1="0" y2="1">
        <stop offset="0" stopColor={color} stopOpacity=".25"/><stop offset="1" stopColor={color} stopOpacity="0"/>
      </linearGradient></defs>
      {area && <path d={a} fill={`url(#${gid})`}/>}
      <path d={d} fill="none" stroke={color} strokeWidth="1.8" strokeDasharray={dash||"none"} strokeLinejoin="round" strokeLinecap="round"/>
      <circle cx={pts.at(-1)[0]} cy={pts.at(-1)[1]} r="3" fill={color}/>
    </svg>
  );
}

function StatCard({eyebrow, value, sub, spark, trend, color, icon, prefix="Rs ", delta}) {
  const IcC = icon ? Icon[icon] : null;
  return (
    <div className="card-machined" style={{padding:18,display:"flex",flexDirection:"column",gap:12,position:"relative",overflow:"hidden"}}>
      <div style={{display:"flex",alignItems:"center",justifyContent:"space-between"}}>
        <div className="eyebrow">{eyebrow}</div>
        {IcC && <div style={{width:34,height:34,borderRadius:10,background:"var(--raised-2)",display:"flex",alignItems:"center",justifyContent:"center",border:"1px solid var(--hairline)"}}><IcC size={16}/></div>}
      </div>
      <div className="num" style={{fontSize:32,fontWeight:800,letterSpacing:"-0.025em",lineHeight:1}}>
        {prefix}<CountUp value={value} decimals={typeof sub==="number"?0:2}/>
        {typeof sub==="string" && <span style={{fontSize:16,color:"var(--muted)",fontWeight:700,marginLeft:6}}>{sub}</span>}
      </div>
      <div style={{display:"flex",alignItems:"center",justifyContent:"space-between"}}>
        <div style={{display:"inline-flex",alignItems:"center",gap:6,fontSize:12,color:trend>=0?"var(--emerald)":"var(--crimson)",fontWeight:600}}>
          {trend>=0?<Icon.arrowup size={12}/>:<Icon.arrowdn size={12}/>}
          <span className="num">{Math.abs(trend)}%</span>
          <span style={{color:"var(--muted)",fontWeight:500}}>vs last week</span>
        </div>
        {spark && <Spark data={spark} color={color||"var(--amber)"} w={90} h={28}/>}
      </div>
    </div>
  );
}

function QuickTile({label, icon, color="var(--ink)", onClick}) {
  const IcC = Icon[icon];
  return (
    <button onClick={onClick} className="card-machined" style={{
      padding:14, cursor:"pointer", textAlign:"left", display:"flex", flexDirection:"column", gap:10, minHeight:108,
      border:"1px solid var(--hairline)"
    }}>
      <div style={{width:38,height:38,borderRadius:10,background:color,color:"var(--surface)",display:"flex",alignItems:"center",justifyContent:"center"}}>
        <IcC size={18}/>
      </div>
      <div style={{fontSize:13,fontWeight:700,letterSpacing:"-0.005em",lineHeight:1.25}}>{label}</div>
    </button>
  );
}

function Dashboard({ theme, onTheme, onGoTo }) {
  const max = Math.max(...SALES, ...PURCH);
  const W = 720, H = 220, P = 14;
  const step = (W - P*2) / (SALES.length-1);
  const path = (arr) => arr.map((v,i)=>{
    const x = P + i*step;
    const y = H - P - (v/max)*(H - P*2);
    return (i?"L":"M")+x.toFixed(1)+","+y.toFixed(1);
  }).join(" ");
  const areaPath = (arr) => `${path(arr)} L ${P+(arr.length-1)*step},${H-P} L ${P},${H-P} Z`;

  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="home"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0,overflow:"hidden"}}>
        <AppBar
          title="Good afternoon, Sameer"
          subtitle="Wed 26 May · 14:08 · 4 active counters"
          right={
            <div style={{display:"flex",gap:8,alignItems:"center"}}>
              <button className="btn btn-secondary btn-sm"><Icon.filter size={14}/> This week</button>
              <button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
              <button className="btn btn-primary btn-sm" onClick={()=>onGoTo&&onGoTo("pos")}><Icon.cart size={14}/> Open POS</button>
            </div>
          }/>

        <div className="scroll reveal" style={{flex:1,overflow:"auto",padding:"18px 22px 28px",display:"flex",flexDirection:"column",gap:18}}>

          {/* KPI Quick band */}
          <div style={{display:"grid",gridTemplateColumns:"repeat(4, 1fr)",gap:14}}>
            <StatCard eyebrow="Today’s Sales" value={68420} trend={12} spark={SALES.slice(-7)} color="var(--amber)" icon="cart"/>
            <StatCard eyebrow="Stock Value" value={130590} trend={-2} spark={PURCH.slice(-7)} color="var(--ink)" icon="box"/>
            <StatCard eyebrow="Total Due" value={42180} trend={8} spark={[20,25,28,30,32,38,42]} color="var(--crimson)" icon="wallet"/>
            <StatCard eyebrow="Profit (week)" value={88200} trend={18} spark={[40,50,55,52,68,78,88]} color="var(--emerald)" icon="chart"/>
          </div>

          {/* Chart + Top items */}
          <div style={{display:"grid",gridTemplateColumns:"1.65fr 1fr",gap:14}}>
            <div className="card-machined" style={{padding:18,display:"flex",flexDirection:"column",gap:14}}>
              <div style={{display:"flex",alignItems:"flex-end",justifyContent:"space-between"}}>
                <div>
                  <div className="eyebrow">Sales vs Purchase</div>
                  <h2 style={{margin:"4px 0 0",fontSize:20,fontWeight:700,letterSpacing:"-0.015em"}}>14-day movement</h2>
                </div>
                <div style={{display:"flex",gap:14,alignItems:"center"}}>
                  <Legend swatch="var(--amber)" label="Sales" value="Rs 1.42M"/>
                  <Legend swatch="var(--ink)" label="Purchase" value="Rs 1.04M" dashed/>
                </div>
              </div>

              <svg viewBox={`0 0 ${W} ${H+24}`} style={{width:"100%",height:H+24}}>
                <defs>
                  <linearGradient id="sales-fill" x1="0" x2="0" y1="0" y2="1">
                    <stop offset="0" stopColor="var(--amber)" stopOpacity=".25"/><stop offset="1" stopColor="var(--amber)" stopOpacity="0"/>
                  </linearGradient>
                </defs>
                {/* gridlines */}
                {[0,1,2,3,4].map(i=>{
                  const y = P + (i*(H-P*2)/4);
                  return <line key={i} x1={P} x2={W-P} y1={y} y2={y} stroke="var(--hairline)" strokeDasharray="2 4"/>;
                })}
                {/* purchase line (dashed graphite) */}
                <path d={path(PURCH)} fill="none" stroke="var(--ink)" strokeWidth="1.6" strokeDasharray="3 5" strokeLinejoin="round"/>
                {/* sales area */}
                <path d={areaPath(SALES)} fill="url(#sales-fill)"/>
                <path d={path(SALES)} fill="none" stroke="var(--amber)" strokeWidth="2.4" strokeLinejoin="round" strokeLinecap="round"/>
                {/* end dots */}
                <circle cx={P+(SALES.length-1)*step} cy={H-P-(SALES.at(-1)/max)*(H-P*2)} r="4.5" fill="var(--amber)" stroke="var(--surface)" strokeWidth="2"/>
                <circle cx={P+(PURCH.length-1)*step} cy={H-P-(PURCH.at(-1)/max)*(H-P*2)} r="3.5" fill="var(--ink)" stroke="var(--surface)" strokeWidth="2"/>
                {/* x labels */}
                {DAYS.map((d,i)=>(
                  <text key={i} x={P+i*step} y={H+14} textAnchor="middle" fontFamily="JetBrains Mono" fontSize="10" fill="var(--muted)">{d}</text>
                ))}
                {/* highlight today */}
                <line x1={P+(SALES.length-1)*step} x2={P+(SALES.length-1)*step} y1={P} y2={H-P} stroke="var(--amber)" strokeWidth="1" strokeDasharray="2 3" opacity=".4"/>
              </svg>

              <div style={{display:"grid",gridTemplateColumns:"repeat(4, 1fr)",gap:12,paddingTop:6,borderTop:"1px solid var(--hairline-2)"}}>
                <Mini label="Avg ticket" value="Rs 1,184" delta="+4%"/>
                <Mini label="Tickets" value="58" delta="+12%"/>
                <Mini label="Returns" value="Rs 2,140" delta="-22%" down/>
                <Mini label="Margin" value="34.2%" delta="+1.4pt"/>
              </div>
            </div>

            <div className="card-machined" style={{padding:18,display:"flex",flexDirection:"column",gap:10}}>
              <div style={{display:"flex",justifyContent:"space-between",alignItems:"baseline"}}>
                <div>
                  <div className="eyebrow">Top selling · this week</div>
                  <h2 style={{margin:"4px 0 0",fontSize:18,fontWeight:700}}>Movers</h2>
                </div>
                <button className="btn btn-ghost btn-sm">View all <Icon.chev_r size={14}/></button>
              </div>
              {[
                {n:"Sprayer 20L Hi-Pressure", k:"sprayer", qty:18, rev:22500, share:.84},
                {n:"Viseuse Cordless 18V",     k:"drill",   qty:9,  rev:13050, share:.49},
                {n:"Paint Enamel 5L Brick",    k:"paint",   qty:14, rev:16520, share:.62},
                {n:'Saw Chain 18"',            k:"saw",     qty:7,  rev:5950,  share:.22},
                {n:"Frottoire Gros Malaysia",  k:"scrubber",qty:24, rev:5880,  share:.22},
              ].map((r,i)=>(
                <div key={i} style={{display:"flex",alignItems:"center",gap:10,padding:"8px 0",borderBottom:"1px solid var(--hairline-2)"}}>
                  <div style={{width:36,height:36,borderRadius:8,background:"var(--raised-2)",overflow:"hidden",border:"1px solid var(--hairline-2)",display:"flex",alignItems:"center",justifyContent:"center"}}>
                    <ProductTile kind={r.k} size={34}/>
                  </div>
                  <div style={{flex:1,minWidth:0}}>
                    <div style={{fontSize:13,fontWeight:600,whiteSpace:"nowrap",overflow:"hidden",textOverflow:"ellipsis"}}>{r.n}</div>
                    <div style={{height:4,background:"var(--raised-2)",borderRadius:99,marginTop:4,overflow:"hidden"}}>
                      <div style={{height:"100%",width: (r.share*100)+"%",background:"var(--amber)",borderRadius:99}}/>
                    </div>
                  </div>
                  <div style={{textAlign:"right"}}>
                    <div className="num" style={{fontSize:13,fontWeight:700}}>Rs {r.rev.toLocaleString()}</div>
                    <div className="num" style={{fontSize:10.5,color:"var(--muted)"}}>× {r.qty}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Inventory band */}
          <div style={{display:"grid",gridTemplateColumns:"repeat(4, 1fr)",gap:14}}>
            <StatCard eyebrow="Items in stock" value={62} prefix="" sub="SKUs" trend={3} color="var(--ink)" icon="box" spark={[10,12,15,16,18,20,22]}/>
            <StatCard eyebrow="Categories" value={12} prefix="" sub="" trend={0} color="var(--graphite)" icon="filter" spark={[8,8,9,10,10,11,12]}/>
            <StatCard eyebrow="Low stock" value={7} prefix="" sub="alerts" trend={40} color="var(--low)" icon="bell" spark={[2,3,4,5,5,6,7]}/>
            <StatCard eyebrow="Suppliers" value={18} prefix="" sub="active" trend={5} color="var(--graphite)" icon="truck" spark={[12,13,14,16,17,18,18]}/>
          </div>

          {/* Quick actions + Activity */}
          <div style={{display:"grid",gridTemplateColumns:"1.4fr 1fr",gap:14}}>
            <div>
              <div className="eyebrow" style={{marginBottom:8}}>Quick actions</div>
              <div style={{display:"grid",gridTemplateColumns:"repeat(5,1fr)",gap:10}}>
                <QuickTile label="New sale" icon="cart" color="var(--amber)" onClick={()=>onGoTo&&onGoTo("pos")}/>
                <QuickTile label="Add product" icon="box" color="var(--ink)" onClick={()=>onGoTo&&onGoTo("add-product")}/>
                <QuickTile label="Record expense" icon="wallet" color="var(--crimson)" onClick={()=>onGoTo&&onGoTo("add-expense")}/>
                <QuickTile label="Purchase from supplier" icon="truck" color="var(--graphite)" onClick={()=>onGoTo&&onGoTo("add-purchase")}/>
                <QuickTile label="Print labels" icon="barcode" color="var(--ink)" onClick={()=>onGoTo&&onGoTo("products")}/>
              </div>
            </div>
            <div className="card-machined" style={{padding:16}}>
              <div className="eyebrow">Live activity</div>
              <div style={{marginTop:10,display:"flex",flexDirection:"column",gap:10}}>
                {[
                  {t:"S-00009 paid · Rs 3,240", who:"Cash · Counter 02", time:"2m", color:"var(--emerald)"},
                  {t:"Low stock: PVC Pipe 110mm", who:"3 left in Rack B-04", time:"5m", color:"var(--low)"},
                  {t:"Purchase P-1042 received", who:"Ducray Hardware Ltd", time:"22m", color:"var(--ink)"},
                  {t:"Return R-007 · Rs 480", who:"Refund to store credit", time:"1h", color:"var(--crimson)"},
                ].map((a,i)=>(
                  <div key={i} style={{display:"flex",alignItems:"flex-start",gap:10}}>
                    <span style={{width:8,height:8,marginTop:6,borderRadius:99,background:a.color,flexShrink:0}}/>
                    <div style={{flex:1}}>
                      <div style={{fontSize:13,fontWeight:600,lineHeight:1.25}}>{a.t}</div>
                      <div style={{fontSize:11,color:"var(--muted)",fontFamily:"var(--mono)"}}>{a.who}</div>
                    </div>
                    <span style={{fontSize:11,color:"var(--muted)",fontFamily:"var(--mono)"}}>{a.time}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
}

function Legend({swatch,label,value,dashed}) {
  return (
    <div style={{display:"flex",alignItems:"center",gap:8}}>
      {dashed
        ? <svg width="20" height="6"><line x1="0" y1="3" x2="20" y2="3" stroke={swatch} strokeWidth="2" strokeDasharray="3 4"/></svg>
        : <span style={{width:14,height:3,background:swatch,borderRadius:2,display:"inline-block"}}/>}
      <span style={{fontSize:12,color:"var(--muted)",fontWeight:600}}>{label}</span>
      <span className="num" style={{fontSize:13,fontWeight:700}}>{value}</span>
    </div>
  );
}
function Mini({label,value,delta,down}) {
  return (
    <div>
      <div style={{fontSize:11,color:"var(--muted)",fontWeight:600,letterSpacing:".04em",textTransform:"uppercase"}}>{label}</div>
      <div style={{display:"flex",alignItems:"baseline",gap:6,marginTop:4}}>
        <span className="num" style={{fontSize:16,fontWeight:700}}>{value}</span>
        <span className="num" style={{fontSize:11,color: down?"var(--crimson)":"var(--emerald)",fontWeight:600}}>{delta}</span>
      </div>
    </div>
  );
}

Object.assign(window, { Dashboard });
