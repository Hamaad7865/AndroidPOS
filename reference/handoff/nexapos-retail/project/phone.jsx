/* phone.jsx — phone (portrait) variants: POS, Dashboard, Receipt */
const { useState: upHS } = React;

function PhoneFrame({ children, theme="light" }) {
  return (
    <div className="phone-frame" data-theme={theme}>
      <div className="screen" style={{background:"var(--bg)",color:"var(--ink)"}}>
        {children}
      </div>
    </div>
  );
}
function PhoneStatus({ theme, dark }) {
  return (
    <div style={{
      height:42, display:"flex", alignItems:"flex-end", justifyContent:"space-between",
      padding:"0 22px 6px", fontFamily:"var(--mono)", fontSize:13, fontWeight:600, color:"var(--ink)"
    }}>
      <span>14:08</span>
      <span style={{display:"inline-flex",alignItems:"center",gap:6}}>
        <svg width="14" height="10" viewBox="0 0 14 10" fill="currentColor"><path d="M0 8h2v2H0zM4 6h2v4H4zM8 3h2v7H8zM12 0h2v10h-2z"/></svg>
        <svg width="14" height="10" viewBox="0 0 14 10" fill="none" stroke="currentColor" strokeWidth="1.4"><path d="M1 4a8 8 0 0 1 12 0M3.5 6a5 5 0 0 1 7 0M6 8a2 2 0 0 1 2 0"/></svg>
        <span style={{display:"inline-flex",alignItems:"center"}}>
          <span style={{width:22,height:10,border:"1.5px solid currentColor",borderRadius:2,position:"relative"}}>
            <span style={{position:"absolute",inset:1,right:"15%",background:"currentColor",borderRadius:1}}/>
          </span>
        </span>
      </span>
    </div>
  );
}

function PhoneTab({active="pos", onChange=()=>{}}) {
  const items = [
    {id:"home",l:"Home",ic:"home"},
    {id:"pos",l:"POS",ic:"cart"},
    {id:"dash",l:"Dash",ic:"chart"},
    {id:"reports",l:"Reports",ic:"report"},
    {id:"settings",l:"More",ic:"setting"},
  ];
  return (
    <div style={{
      position:"absolute", bottom:0, left:0, right:0,
      background:"var(--surface)", borderTop:"1px solid var(--hairline)",
      paddingBottom: 22, paddingTop: 8,
      display:"flex", justifyContent:"space-around",
    }}>
      {items.map(it=>{
        const on = active===it.id; const IcC = Icon[it.ic];
        return (
          <button key={it.id} onClick={()=>onChange(it.id)} style={{
            background:"transparent",border:0,cursor:"pointer",
            display:"flex",flexDirection:"column",alignItems:"center",gap:3,
            color: on?"var(--ink)":"var(--muted)"
          }}>
            <div style={{
              width:52,height:28,borderRadius:99,display:"flex",alignItems:"center",justifyContent:"center",
              background: on?"var(--amber-soft)":"transparent",color: on?"var(--amber-press)":"var(--muted)"
            }}><IcC size={18}/></div>
            <span style={{fontSize:11,fontWeight: on?700:500}}>{it.l}</span>
          </button>
        );
      })}
    </div>
  );
}

/* ---------- Phone POS ---------- */
function PhonePOS({ theme }) {
  const [cartOpen, setCartOpen] = upHS(true);
  return (
    <PhoneFrame theme={theme}>
      <PhoneStatus/>
      <div style={{padding:"4px 16px 14px"}}>
        <div style={{display:"flex",alignItems:"center",justifyContent:"space-between"}}>
          <div>
            <div className="eyebrow">Counter 01 · POS</div>
            <h1 style={{margin:"2px 0 0",fontSize:22,fontWeight:800,letterSpacing:"-0.02em"}}>POS Sale</h1>
          </div>
          <div style={{display:"flex",gap:6}}>
            <button className="btn btn-secondary" style={{height:36,width:36,padding:0,borderRadius:10}}><Icon.scan size={16}/></button>
            <button className="btn btn-secondary" style={{height:36,width:36,padding:0,borderRadius:10}}><Icon.user size={16}/></button>
          </div>
        </div>
      </div>
      <div style={{padding:"0 16px 10px"}}>
        <div className="field" style={{height:42}}><Icon.search size={16}/><input placeholder="Search · scan barcode"/></div>
      </div>
      <div style={{padding:"0 16px 10px",display:"flex",gap:6,overflowX:"auto"}} className="scroll">
        {["All","Tools","Plumbing","Paint","Fasteners"].map((c,i)=>(
          <button key={c} className={"chip"+(i===0?" active":"")} style={{height:32,fontSize:12,padding:"0 10px"}}>{c}</button>
        ))}
      </div>

      <div className="scroll reveal" style={{padding:"0 16px 100px",overflow:"auto",height: cartOpen ? 280 : 510, display:"grid",gridTemplateColumns:"1fr 1fr",gap:10}}>
        {PRODUCTS.slice(0,8).map(p=>(
          <div key={p.id} className="card-machined" style={{padding:8,position:"relative"}}>
            <div style={{height:80,background:"var(--raised-2)",borderRadius:8,display:"flex",alignItems:"center",justifyContent:"center"}}>
              <ProductTile kind={p.kind} size={64}/>
            </div>
            <div style={{fontSize:11.5,fontWeight:600,marginTop:6,lineHeight:1.2,display:"-webkit-box",WebkitLineClamp:2,WebkitBoxOrient:"vertical",overflow:"hidden",minHeight:28}}>{p.name}</div>
            <div className="num" style={{fontSize:14,fontWeight:700,marginTop:2}}>Rs {p.price.toLocaleString()}</div>
          </div>
        ))}
      </div>

      {/* Sticky cart bar */}
      {!cartOpen && (
        <div onClick={()=>setCartOpen(true)} style={{
          position:"absolute",bottom:84,left:12,right:12,
          background:"var(--ink)",color:"var(--surface)",borderRadius:14,padding:"12px 14px",
          display:"flex",alignItems:"center",gap:10,boxShadow:"var(--shadow-3)",cursor:"pointer"
        }}>
          <span style={{width:30,height:30,borderRadius:99,background:"var(--amber)",display:"flex",alignItems:"center",justifyContent:"center",fontWeight:800,fontFamily:"var(--mono)",fontSize:13}}>3</span>
          <div style={{flex:1}}>
            <div style={{fontSize:12,opacity:.7,letterSpacing:".06em",textTransform:"uppercase",fontWeight:600}}>Ticket S-00010</div>
            <div className="num" style={{fontSize:18,fontWeight:800}}>Rs 2,335</div>
          </div>
          <span style={{padding:"6px 12px",background:"var(--amber)",borderRadius:8,fontWeight:700,fontSize:13}}>Charge ›</span>
        </div>
      )}

      {/* Bottom-sheet ticket */}
      {cartOpen && (
        <div style={{
          position:"absolute",bottom:0,left:0,right:0,
          background:"var(--surface)",borderTopLeftRadius:24,borderTopRightRadius:24,
          padding:"14px 16px 84px",borderTop:"1px solid var(--hairline)",
          boxShadow:"0 -20px 50px rgba(0,0,0,0.15)",
        }}>
          <div onClick={()=>setCartOpen(false)} style={{width:42,height:5,background:"var(--hairline)",borderRadius:99,margin:"0 auto 12px",cursor:"pointer"}}/>
          <div style={{display:"flex",justifyContent:"space-between",alignItems:"baseline"}}>
            <div>
              <div className="eyebrow">Ticket S-00010</div>
              <div style={{fontSize:13,marginTop:2,color:"var(--muted)"}}>Ravi Soobramoney · 3 items</div>
            </div>
            <span className="badge badge-amber">● Open</span>
          </div>
          <div style={{marginTop:10,maxHeight:130,overflow:"auto"}} className="scroll">
            {[
              {n:"T-Type Wrench 17mm", k:"wrench", q:2, p:185},
              {n:"Frottoire Gros Malaysia", k:"scrubber", q:1, p:245},
              {n:"Sprayer 20L Hi-Pressure", k:"sprayer", q:1, p:1250},
            ].map((l,i)=>(
              <div key={i} style={{display:"flex",alignItems:"center",gap:8,padding:"6px 0",borderBottom:"1px solid var(--hairline-2)"}}>
                <div style={{width:32,height:32,borderRadius:6,background:"var(--raised-2)",overflow:"hidden",display:"flex",alignItems:"center",justifyContent:"center"}}>
                  <ProductTile kind={l.k} size={28}/>
                </div>
                <div style={{flex:1,minWidth:0}}>
                  <div style={{fontSize:12,fontWeight:600,whiteSpace:"nowrap",overflow:"hidden",textOverflow:"ellipsis"}}>{l.n}</div>
                  <div className="num" style={{fontSize:10.5,color:"var(--muted)"}}>{l.q} × Rs {l.p}</div>
                </div>
                <div className="num" style={{fontSize:13,fontWeight:700}}>Rs {(l.q*l.p).toLocaleString()}</div>
              </div>
            ))}
          </div>
          <div style={{display:"flex",justifyContent:"space-between",marginTop:8,fontSize:12,color:"var(--muted)"}}>
            <span>Subtotal</span><span className="num">Rs 1,865</span>
          </div>
          <div style={{display:"flex",justifyContent:"space-between",fontSize:12,color:"var(--muted)"}}>
            <span>VAT 15%</span><span className="num">Rs 280</span>
          </div>
          <div style={{display:"flex",justifyContent:"space-between",alignItems:"baseline",marginTop:6}}>
            <span style={{fontSize:11,letterSpacing:".06em",textTransform:"uppercase",fontWeight:700,color:"var(--muted)"}}>Total</span>
            <span className="num" style={{fontSize:26,fontWeight:800,letterSpacing:"-0.02em"}}>Rs 2,145</span>
          </div>
          <button className="btn btn-primary btn-lg" style={{width:"100%",marginTop:10}}>Charge Rs 2,145 <Icon.arrow_r size={18}/></button>
        </div>
      )}

      <PhoneTab active="pos"/>
    </PhoneFrame>
  );
}

/* ---------- Phone Dashboard ---------- */
function PhoneDashboard({ theme }) {
  return (
    <PhoneFrame theme={theme}>
      <PhoneStatus/>
      <div className="scroll" style={{height:"calc(100% - 70px)",overflow:"auto",padding:"4px 16px 90px"}}>
        <div style={{display:"flex",alignItems:"center",justifyContent:"space-between"}}>
          <div>
            <div className="eyebrow">Wed · 26 May</div>
            <h1 style={{margin:"2px 0 0",fontSize:22,fontWeight:800,letterSpacing:"-0.02em"}}>Hi, Sameer</h1>
          </div>
          <div style={{width:38,height:38,borderRadius:99,background:"var(--amber-soft)",color:"var(--amber-press)",display:"flex",alignItems:"center",justifyContent:"center",fontFamily:"var(--mono)",fontWeight:700,fontSize:13}}>SK</div>
        </div>

        <div className="card-machined" style={{padding:14,marginTop:12,background:"var(--ink)",color:"var(--surface)",borderColor:"transparent",position:"relative",overflow:"hidden"}}>
          <div className="eyebrow" style={{color:"rgba(244,236,221,0.6)"}}>Today’s sales</div>
          <div className="num" style={{fontSize:34,fontWeight:800,marginTop:4,letterSpacing:"-0.02em"}}>Rs 68,420</div>
          <div style={{display:"inline-flex",alignItems:"center",gap:5,marginTop:6,color:"var(--amber)",fontWeight:600,fontSize:12}}>
            <Icon.arrowup size={12}/> <span className="num">12%</span> <span style={{opacity:.7,fontWeight:500,color:"rgba(244,236,221,0.6)"}}>vs yesterday</span>
          </div>
          <div style={{marginTop:10}}>
            <svg viewBox="0 0 280 60" width="100%" height="60">
              <path d={linePath([20,28,24,40,36,52,48,60,55,72,68,80,90], 280, 60)} fill="none" stroke="var(--amber)" strokeWidth="2.2"/>
            </svg>
          </div>
        </div>

        <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:10,marginTop:10}}>
          <MiniStat eyebrow="Stock value" v="Rs 130.59K" t={-2}/>
          <MiniStat eyebrow="Total due" v="Rs 42.18K" t={8} red/>
          <MiniStat eyebrow="Items" v="62" t={3}/>
          <MiniStat eyebrow="Low stock" v="7" t={40} amber/>
        </div>

        <div style={{display:"flex",alignItems:"center",justifyContent:"space-between",marginTop:18}}>
          <div className="eyebrow">Quick actions</div>
          <span style={{fontSize:11,color:"var(--muted)"}}>Tap to start</span>
        </div>
        <div style={{display:"grid",gridTemplateColumns:"repeat(4,1fr)",gap:8,marginTop:8}}>
          {[
            {ic:"cart",l:"Sale",c:"var(--amber)"},
            {ic:"box",l:"Add",c:"var(--ink)"},
            {ic:"truck",l:"Buy",c:"var(--graphite)"},
            {ic:"wallet",l:"Exp",c:"var(--crimson)"},
          ].map((q,i)=>{const IcC = Icon[q.ic]; return (
            <button key={i} className="card-machined" style={{padding:10,display:"flex",flexDirection:"column",alignItems:"flex-start",gap:8}}>
              <div style={{width:30,height:30,borderRadius:8,background:q.c,color:"var(--surface)",display:"flex",alignItems:"center",justifyContent:"center"}}>
                <IcC size={14}/>
              </div>
              <div style={{fontSize:12,fontWeight:700}}>{q.l}</div>
            </button>
          )})}
        </div>

        <div style={{marginTop:14}}>
          <div className="eyebrow" style={{marginBottom:8}}>Live activity</div>
          <div className="card" style={{padding:12,display:"flex",flexDirection:"column",gap:10}}>
            {[
              {t:"S-00009 paid · Rs 3,240",who:"Cash · Counter 02",time:"2m",c:"var(--emerald)"},
              {t:"PVC Pipe 110mm · 3 left",who:"Rack B-04",time:"5m",c:"var(--low)"},
              {t:"Purchase P-1042 received",who:"Ducray Hardware",time:"22m",c:"var(--ink)"},
            ].map((a,i)=>(
              <div key={i} style={{display:"flex",gap:8,alignItems:"flex-start"}}>
                <span style={{width:7,height:7,borderRadius:99,background:a.c,marginTop:6}}/>
                <div style={{flex:1}}>
                  <div style={{fontSize:12.5,fontWeight:600}}>{a.t}</div>
                  <div style={{fontSize:10.5,color:"var(--muted)",fontFamily:"var(--mono)"}}>{a.who}</div>
                </div>
                <span style={{fontSize:10.5,color:"var(--muted)",fontFamily:"var(--mono)"}}>{a.time}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
      <PhoneTab active="home"/>
    </PhoneFrame>
  );
}
function MiniStat({eyebrow,v,t,red,amber}) {
  const c = red?"var(--crimson)":amber?"var(--low)":"var(--emerald)";
  return (
    <div className="card-machined" style={{padding:12}}>
      <div className="eyebrow" style={{fontSize:10}}>{eyebrow}</div>
      <div className="num" style={{fontSize:18,fontWeight:800,marginTop:4,letterSpacing:"-0.015em"}}>{v}</div>
      <div style={{display:"inline-flex",alignItems:"center",gap:4,marginTop:4,color:c,fontSize:11,fontWeight:600}}>
        {t>=0?<Icon.arrowup size={10}/>:<Icon.arrowdn size={10}/>} <span className="num">{Math.abs(t)}%</span>
      </div>
    </div>
  );
}

/* ---------- Phone Receipt ---------- */
function PhoneReceipt({ theme }) {
  return (
    <PhoneFrame theme={theme}>
      <PhoneStatus/>
      <div style={{padding:"8px 16px 14px",display:"flex",alignItems:"center",gap:10}}>
        <button className="btn btn-ghost" style={{width:36,height:36,padding:0,borderRadius:99}}><Icon.close size={16}/></button>
        <div style={{flex:1}}>
          <div className="eyebrow">Sale complete</div>
          <div style={{fontSize:13,fontWeight:700}}>S-00010 · paid</div>
        </div>
        <button className="btn btn-secondary" style={{width:36,height:36,padding:0,borderRadius:99}}><Icon.share size={16}/></button>
      </div>

      <div style={{padding:"0 16px",display:"flex",flexDirection:"column",gap:14,alignItems:"center"}}>
        <div style={{display:"flex",alignItems:"center",gap:8,color:"var(--emerald)",fontSize:12,fontWeight:700,letterSpacing:".06em",textTransform:"uppercase"}}>
          <span style={{width:8,height:8,borderRadius:99,background:"var(--emerald)"}}/> Payment confirmed
        </div>
        <div className="num" style={{fontSize:44,fontWeight:800,letterSpacing:"-0.03em",lineHeight:1}}>Rs 2,145</div>
        <div style={{fontSize:12,color:"var(--muted)",fontFamily:"var(--mono)"}}>Cash · change Rs 855</div>

        <div style={{
          background:"#FBF7EE", color:"#14110C", width:"100%",
          padding:"16px 18px 26px",borderRadius:8,boxShadow:"var(--shadow-3)",
          fontFamily:"var(--mono)", fontSize:11, position:"relative",
          backgroundImage:"var(--grain)",backgroundBlendMode:"multiply",
          clipPath:"polygon(0 0, 100% 0, 100% calc(100% - 8px), 96% 100%, 92% calc(100% - 6px), 88% 100%, 84% calc(100% - 6px), 80% 100%, 76% calc(100% - 6px), 72% 100%, 68% calc(100% - 6px), 64% 100%, 60% calc(100% - 6px), 56% 100%, 52% calc(100% - 6px), 48% 100%, 44% calc(100% - 6px), 40% 100%, 36% calc(100% - 6px), 32% 100%, 28% calc(100% - 6px), 24% 100%, 20% calc(100% - 6px), 16% 100%, 12% calc(100% - 6px), 8% 100%, 4% calc(100% - 6px), 0 100%)"
        }}>
          <div style={{textAlign:"center",fontFamily:"var(--display)",fontWeight:800,fontSize:14}}>QUINCAILLERIE RB TRADING</div>
          <div style={{textAlign:"center",fontSize:10,color:"#5b5246",marginTop:1}}>Royal Rd, Curepipe · VAT20188822</div>
          <div style={{borderTop:"1px dashed #c8bda5",margin:"10px 0"}}/>
          <div style={{display:"flex",justifyContent:"space-between"}}><span>S-00010</span><span>26 May 14:08</span></div>
          <div style={{borderTop:"1px dashed #c8bda5",margin:"10px 0"}}/>
          <Rr l="Wrench 17mm × 2" v={370}/>
          <Rr l="Frottoire × 1" v={245}/>
          <Rr l="Sprayer 20L × 1" v={1250}/>
          <div style={{borderTop:"1px dashed #c8bda5",margin:"8px 0"}}/>
          <Rr l="Subtotal" v={1865}/>
          <Rr l="VAT 15%" v={280}/>
          <div style={{borderTop:"1px solid #14110C",margin:"6px 0"}}/>
          <Rr l="TOTAL" v={2145} big/>
          <div style={{position:"absolute",top:54,right:18,border:"2.5px solid var(--emerald)",color:"var(--emerald)",padding:"3px 10px",borderRadius:5,fontFamily:"var(--display)",fontWeight:800,letterSpacing:".08em",fontSize:14,transform:"rotate(-12deg)"}}>PAID</div>
        </div>

        <div style={{display:"flex",gap:8,width:"100%"}}>
          <button className="btn btn-primary btn-lg" style={{flex:1}}><Icon.print size={16}/> Print</button>
          <button className="btn btn-secondary btn-lg" style={{flex:1}}><Icon.share size={16}/> Send</button>
        </div>
      </div>
    </PhoneFrame>
  );
}

Object.assign(window, { PhoneFrame, PhonePOS, PhoneDashboard, PhoneReceipt, PhoneStatus });
