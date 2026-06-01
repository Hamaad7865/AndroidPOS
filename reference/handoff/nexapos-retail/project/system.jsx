/* system.jsx — Design system reference page (Workshop Precision) */
function DesignSystem({ theme, onTheme }) {
  return (
    <div style={{height:"100%",overflow:"auto",background:"var(--bg)",color:"var(--ink)"}} className="scroll">
      <div style={{maxWidth:1180,margin:"0 auto",padding:"36px 32px 80px"}}>
        {/* Header */}
        <div style={{display:"flex",alignItems:"flex-start",justifyContent:"space-between",gap:20}}>
          <div>
            <div style={{display:"flex",alignItems:"center",gap:10,marginBottom:14}}>
              <Icon.logo size={36}/>
              <div>
                <div style={{fontSize:11,letterSpacing:".2em",color:"var(--amber)",fontWeight:700}}>NEXAPOS · DESIGN SYSTEM</div>
                <div style={{fontSize:14,color:"var(--muted)",fontFamily:"var(--mono)"}}>v 2.4 · 2026-05-26</div>
              </div>
            </div>
            <h1 style={{margin:0,fontSize:64,fontWeight:800,letterSpacing:"-0.03em",lineHeight:.95}}>
              Workshop<br/>
              <span style={{color:"var(--amber)"}}>Precision.</span>
            </h1>
            <p style={{maxWidth:620,fontSize:16,color:"var(--graphite)",lineHeight:1.5,marginTop:18}}>
              High-end fintech clarity meets the tactile, engineered feel of quality tools. Crisp, calm, precise, quietly premium — built for hardware-store counters that close the day-book before the gate rolls down.
            </p>
          </div>
          <button className="btn btn-secondary" onClick={onTheme}>
            {theme==="light"?<Icon.moon size={16}/>:<Icon.sun size={16}/>} {theme==="light"?"Counter Mode":"Daylight"}
          </button>
        </div>

        {/* Principles */}
        <div style={{display:"grid",gridTemplateColumns:"repeat(4,1fr)",gap:14,marginTop:28}}>
          {[
            {t:"Speed first",d:"Add → charge in 1–2 taps. Numbers are heroes."},
            {t:"Tactile",d:"Machined cards, hairline edges, fine grain."},
            {t:"Calm",d:"One signature accent, dense but never crowded."},
            {t:"Trustworthy",d:"Tabular numerals everywhere, never approximate."},
          ].map((p,i)=>(
            <div key={i} className="card-machined" style={{padding:16}}>
              <div style={{fontSize:13,letterSpacing:".06em",textTransform:"uppercase",color:"var(--amber)",fontWeight:700}}>{String(i+1).padStart(2,"0")}</div>
              <div style={{fontSize:18,fontWeight:700,marginTop:4,letterSpacing:"-0.01em"}}>{p.t}</div>
              <div style={{fontSize:12,color:"var(--muted)",marginTop:4,lineHeight:1.4}}>{p.d}</div>
            </div>
          ))}
        </div>

        {/* Section: Colour */}
        <Section eyebrow="01 · Surface" title="Colour" sub="Ink graphite & warm paper · one molten-amber signature accent.">
          <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:18}}>
            <PaletteCard title="Daylight" rows={[
              {n:"BG paper",      hex:"#F1ECE2", v:"--bg"},
              {n:"Surface",       hex:"#FBF7EE", v:"--surface"},
              {n:"Raised",        hex:"#FFFFFF", v:"--raised"},
              {n:"Ink",           hex:"#14110C", v:"--ink", dark:true},
              {n:"Graphite",      hex:"#3B342A", v:"--graphite", dark:true},
              {n:"Muted",         hex:"#807665", v:"--muted", dark:true},
              {n:"Hairline",      hex:"#DBD2BE", v:"--hairline"},
            ]}/>
            <PaletteCard title="Counter Mode" rows={[
              {n:"Ground",        hex:"#0A0908", v:"--bg", dark:true},
              {n:"Surface",       hex:"#131110", v:"--surface", dark:true},
              {n:"Raised",        hex:"#1A1714", v:"--raised", dark:true},
              {n:"Ink",           hex:"#F4ECDD", v:"--ink"},
              {n:"Graphite",      hex:"#C7BAA0", v:"--graphite"},
              {n:"Muted",         hex:"#857B68", v:"--muted", dark:true},
              {n:"Hairline",      hex:"#2A2520", v:"--hairline", dark:true},
            ]}/>
          </div>

          <div style={{marginTop:14}}>
            <div className="eyebrow" style={{marginBottom:10}}>Signature & semantic</div>
            <div style={{display:"grid",gridTemplateColumns:"repeat(4, 1fr)",gap:12}}>
              <SemCard name="Signature accent" desc="Molten amber · CTAs, active state" hex="#E8651D"/>
              <SemCard name="Income · paid" desc="Emerald" hex="#18764E"/>
              <SemCard name="Due · loss" desc="Crimson" hex="#B43329"/>
              <SemCard name="Low stock" desc="Warning amber-gold" hex="#C58A18"/>
            </div>
          </div>
        </Section>

        {/* Type */}
        <Section eyebrow="02 · Voice" title="Type" sub="Hanken Grotesk for everything readable. JetBrains Mono for every number that matters.">
          <div className="card-machined" style={{padding:24}}>
            <div style={{display:"grid",gridTemplateColumns:"1fr 200px",gap:24,alignItems:"baseline"}}>
              <div style={{fontSize:88,fontWeight:800,letterSpacing:"-0.035em",lineHeight:.95}}>The till you can hear tick.</div>
              <div style={{fontFamily:"var(--mono)",fontSize:11,letterSpacing:".14em",color:"var(--muted)"}}>HANKEN · 88 / 800 · -3.5%</div>
            </div>
            <hr style={{border:0,borderTop:"1px solid var(--hairline-2)",margin:"24px 0"}}/>
            <TypeRow s={48} w={800} t={-2.5}>Display L · 48/800</TypeRow>
            <TypeRow s={32} w={700} t={-2}>Display M · 32/700</TypeRow>
            <TypeRow s={22} w={700} t={-1.5}>Heading · 22/700</TypeRow>
            <TypeRow s={16} w={500} t={-0.5}>Body · 16/500</TypeRow>
            <TypeRow s={14} w={500} t={0}>UI · 14/500</TypeRow>
            <TypeRow s={11} w={600} t={6} caps>Eyebrow · 11/600 · TRACK 0.06EM</TypeRow>
            <hr style={{border:0,borderTop:"1px solid var(--hairline-2)",margin:"24px 0"}}/>
            <div style={{display:"flex",alignItems:"baseline",justifyContent:"space-between",flexWrap:"wrap",gap:14}}>
              <span className="num" style={{fontSize:64,fontWeight:800,letterSpacing:"-0.025em"}}>Rs 130,590.00</span>
              <span style={{fontFamily:"var(--mono)",fontSize:11,letterSpacing:".14em",color:"var(--muted)"}}>JETBRAINS MONO · TABULAR · 64/800 · -2.5%</span>
            </div>
            <div className="num" style={{marginTop:10,fontSize:14,color:"var(--muted)"}}>0123456789 · S-00010 · 8930041800218 · 14:08:42</div>
          </div>
        </Section>

        {/* Buttons */}
        <Section eyebrow="03 · Action" title="Buttons" sub="Four ranks. Primary uses the signature; one per region.">
          <div className="card-machined" style={{padding:24,display:"flex",flexWrap:"wrap",gap:12,alignItems:"center"}}>
            <button className="btn btn-primary btn-xl">Charge Rs 2,145 <Icon.arrow_r size={20}/></button>
            <button className="btn btn-primary">Confirm sale</button>
            <button className="btn btn-secondary">Add product</button>
            <button className="btn btn-ghost">Cancel</button>
            <button className="btn btn-danger">Delete</button>
            <button className="btn btn-primary btn-sm"><Icon.plus size={14}/> New</button>
            <button className="btn btn-secondary btn-sm">Filter</button>
          </div>
        </Section>

        {/* Inputs + Badges + Chips */}
        <Section eyebrow="04 · Inputs" title="Forms & status" sub="Hairline borders, amber focus ring, mono-numerals.">
          <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:18}}>
            <div className="card-machined" style={{padding:18,display:"flex",flexDirection:"column",gap:12}}>
              <div>
                <div className="label">Customer phone</div>
                <div className="field"><Icon.user size={16}/><input className="num" defaultValue="+230 5712 4408"/></div>
              </div>
              <div>
                <div className="label">Sale price</div>
                <div className="field"><span style={{color:"var(--muted)"}}>Rs</span><input className="num" defaultValue="1,450.00" style={{textAlign:"right",fontWeight:700}}/></div>
              </div>
              <div>
                <div className="label">Notes</div>
                <div className="field" style={{height:"auto",padding:10}}><textarea rows="2" style={{background:"transparent",border:0,outline:0,resize:"none",width:"100%",fontFamily:"var(--display)"}} defaultValue="Bring receipt for warranty."/></div>
              </div>
            </div>
            <div className="card-machined" style={{padding:18}}>
              <div className="eyebrow">Badges</div>
              <div style={{display:"flex",flexWrap:"wrap",gap:8,marginTop:8}}>
                <span className="badge badge-paid">● Paid</span>
                <span className="badge badge-due">● Due Rs 4,420</span>
                <span className="badge badge-low">● 3 left</span>
                <span className="badge badge-amber">● Open</span>
                <span className="badge badge-ghost">Refunded</span>
              </div>
              <div className="eyebrow" style={{marginTop:18}}>Chips</div>
              <div style={{display:"flex",flexWrap:"wrap",gap:8,marginTop:8}}>
                {["All","Tools","Plumbing","Fasteners","Paint","Garden"].map((c,i)=>(
                  <button key={c} className={"chip"+(i===1?" active":"")}>{c}<span className="num" style={{opacity:.7,fontSize:11}}>{[62,18,12,9,8,5][i]}</span></button>
                ))}
              </div>
              <div className="eyebrow" style={{marginTop:18}}>Qty stepper</div>
              <div style={{marginTop:8,display:"flex",alignItems:"center",border:"1px solid var(--hairline)",borderRadius:8,width:"fit-content",overflow:"hidden",background:"var(--raised)"}}>
                <button style={{width:36,height:36,border:0,background:"transparent",cursor:"pointer"}}><Icon.minus size={14}/></button>
                <span className="num" style={{width:36,textAlign:"center",fontWeight:800}}>3</span>
                <button style={{width:36,height:36,border:0,background:"transparent",cursor:"pointer"}}><Icon.plus size={14}/></button>
              </div>
            </div>
          </div>
        </Section>

        {/* Spacing / Radii / Shadows */}
        <Section eyebrow="05 · Spatial" title="Spacing, radii, shadows" sub="8-pt grid · radii 6/10/14/20 · three shadow tiers.">
          <div style={{display:"grid",gridTemplateColumns:"1fr 1fr 1fr",gap:18}}>
            <div className="card-machined" style={{padding:18}}>
              <div className="eyebrow">8-pt spacing</div>
              <div style={{display:"flex",alignItems:"flex-end",gap:8,marginTop:14}}>
                {[4,8,12,16,24,32,48].map(s=>(
                  <div key={s} style={{display:"flex",flexDirection:"column",alignItems:"center",gap:6}}>
                    <div style={{width:s,height:s,background:"var(--amber)",borderRadius:2}}/>
                    <span className="num" style={{fontSize:10,color:"var(--muted)"}}>{s}</span>
                  </div>
                ))}
              </div>
            </div>
            <div className="card-machined" style={{padding:18}}>
              <div className="eyebrow">Radii</div>
              <div style={{display:"flex",gap:12,marginTop:14}}>
                {[{r:6,l:"R2"},{r:10,l:"R3"},{r:14,l:"R4"},{r:20,l:"R5"},{r:999,l:"Pill"}].map(rr=>(
                  <div key={rr.l} style={{flex:1,display:"flex",flexDirection:"column",alignItems:"center",gap:6}}>
                    <div style={{width:"100%",aspectRatio:"1/1",background:"var(--raised-2)",border:"1px solid var(--hairline)",borderRadius:rr.r}}/>
                    <span className="num" style={{fontSize:10,color:"var(--muted)"}}>{rr.l}</span>
                  </div>
                ))}
              </div>
            </div>
            <div className="card-machined" style={{padding:18}}>
              <div className="eyebrow">Shadow tiers</div>
              <div style={{display:"flex",gap:12,marginTop:14}}>
                {[{n:"S1",s:"var(--shadow-1)"},{n:"S2",s:"var(--shadow-2)"},{n:"S3",s:"var(--shadow-3)"},{n:"S4",s:"var(--shadow-4)"}].map(sh=>(
                  <div key={sh.n} style={{flex:1,display:"flex",flexDirection:"column",alignItems:"center",gap:6}}>
                    <div style={{width:"100%",height:60,background:"var(--raised)",borderRadius:10,boxShadow:sh.s,border:"1px solid var(--hairline)"}}/>
                    <span className="num" style={{fontSize:10,color:"var(--muted)"}}>{sh.n}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </Section>

        {/* Iconography */}
        <Section eyebrow="06 · Signs" title="Iconography" sub="1.6px stroke · rounded caps · 24px grid.">
          <div className="card-machined" style={{padding:24}}>
            <div style={{display:"grid",gridTemplateColumns:"repeat(8, 1fr)",gap:14}}>
              {["home","cart","chart","report","box","people","truck","wallet","setting","search","barcode","plus","scan","card","cash","mobile","receipt","print","share","filter","download","star","sun","moon"].map(k=>{
                const IcC = Icon[k];
                return (
                  <div key={k} style={{display:"flex",flexDirection:"column",alignItems:"center",gap:8,padding:"10px 0"}}>
                    <div style={{width:46,height:46,borderRadius:10,background:"var(--raised-2)",border:"1px solid var(--hairline)",display:"flex",alignItems:"center",justifyContent:"center"}}>
                      <IcC size={22}/>
                    </div>
                    <span style={{fontSize:10.5,color:"var(--muted)",fontFamily:"var(--mono)"}}>{k}</span>
                  </div>
                );
              })}
            </div>
          </div>
        </Section>

        {/* States */}
        <Section eyebrow="07 · States" title="Every screen, every state" sub="Empty, loading, error, success, confirmation.">
          <div style={{display:"grid",gridTemplateColumns:"repeat(4,1fr)",gap:14}}>
            <StateCard t="Empty">
              <div style={{display:"flex",flexDirection:"column",alignItems:"center",gap:8,color:"var(--muted)",padding:"24px 0"}}>
                <div style={{width:54,height:54,borderRadius:14,background:"var(--raised-2)",border:"1px dashed var(--hairline)",display:"flex",alignItems:"center",justifyContent:"center"}}>
                  <Icon.cart size={22}/>
                </div>
                <div style={{fontSize:13,fontWeight:700,color:"var(--ink)"}}>No sales yet</div>
                <div style={{fontSize:11,textAlign:"center",maxWidth:160,lineHeight:1.3}}>Open the till to start your first ticket.</div>
              </div>
            </StateCard>
            <StateCard t="Loading">
              <div style={{display:"flex",flexDirection:"column",gap:8,padding:"24px 0"}}>
                {[60,80,40].map((w,i)=>(<div key={i} style={{height:10,width:w+"%",background:"linear-gradient(90deg, var(--raised-2), var(--hairline-2), var(--raised-2))",backgroundSize:"200% 100%",animation:"shimmer 1.4s infinite linear",borderRadius:6}}/>))}
                <style>{`@keyframes shimmer { 0%{background-position:200% 0} 100%{background-position:-200% 0} }`}</style>
              </div>
            </StateCard>
            <StateCard t="Error">
              <div style={{display:"flex",flexDirection:"column",alignItems:"center",gap:8,padding:"24px 0"}}>
                <div style={{width:54,height:54,borderRadius:14,background:"var(--crimson-soft)",color:"var(--crimson)",display:"flex",alignItems:"center",justifyContent:"center"}}>
                  <Icon.close size={22}/>
                </div>
                <div style={{fontSize:13,fontWeight:700}}>Offline</div>
                <div style={{fontSize:11,color:"var(--muted)",textAlign:"center",maxWidth:160,lineHeight:1.3}}>Sales queued. Will sync when the network returns.</div>
              </div>
            </StateCard>
            <StateCard t="Success">
              <div style={{display:"flex",flexDirection:"column",alignItems:"center",gap:8,padding:"24px 0"}}>
                <div style={{width:54,height:54,borderRadius:14,background:"var(--emerald-soft)",color:"var(--emerald)",display:"flex",alignItems:"center",justifyContent:"center"}}>
                  <Icon.check size={26}/>
                </div>
                <div style={{fontSize:13,fontWeight:700}}>Paid</div>
                <div className="num" style={{fontSize:14,color:"var(--muted)"}}>Rs 2,145.00</div>
              </div>
            </StateCard>
          </div>
        </Section>

        {/* Motion */}
        <Section eyebrow="08 · Motion" title="Motion" sub="Tactile but quiet. Easing cubic-bezier(.2,.7,.2,1) at 180–650ms.">
          <div style={{display:"grid",gridTemplateColumns:"1fr 1fr 1fr",gap:14}}>
            {[
              {t:"Add → fly to ticket",d:"Product scales, ripple, +1 chip arcs into the ticket panel. 650ms.",ic:"plus"},
              {t:"Total count-up",d:"Mono digits roll cubic-ease over 600ms when total changes.",ic:"chart"},
              {t:"Sale stamp",d:"PAID stamp pops in -12° rotation, receipt slides up underneath.",ic:"check"},
            ].map((m,i)=>{const IcC=Icon[m.ic]; return (
              <div key={i} className="card-machined" style={{padding:18,display:"flex",flexDirection:"column",gap:10}}>
                <div style={{width:40,height:40,borderRadius:10,background:"var(--amber)",color:"#fff",display:"flex",alignItems:"center",justifyContent:"center"}}>
                  <IcC size={18}/>
                </div>
                <div style={{fontSize:15,fontWeight:700,letterSpacing:"-0.01em"}}>{m.t}</div>
                <div style={{fontSize:12.5,color:"var(--muted)",lineHeight:1.45}}>{m.d}</div>
              </div>
            )})}
          </div>
        </Section>

        <div style={{marginTop:60,paddingTop:24,borderTop:"1px solid var(--hairline)",display:"flex",justifyContent:"space-between",alignItems:"center",color:"var(--muted)",fontSize:12}}>
          <span style={{fontFamily:"var(--mono)",letterSpacing:".14em"}}>NEXAPOS · WORKSHOP PRECISION · 2026</span>
          <span>Crafted for the counter.</span>
        </div>
      </div>
    </div>
  );
}

function Section({eyebrow,title,sub,children}) {
  return (
    <section style={{marginTop:56}}>
      <div style={{marginBottom:18}}>
        <div className="eyebrow">{eyebrow}</div>
        <h2 style={{margin:"6px 0 4px",fontSize:34,fontWeight:800,letterSpacing:"-0.02em"}}>{title}</h2>
        <div style={{fontSize:14,color:"var(--muted)",maxWidth:560}}>{sub}</div>
      </div>
      {children}
    </section>
  );
}
function PaletteCard({title, rows}) {
  return (
    <div className="card-machined" style={{padding:16}}>
      <div className="eyebrow" style={{marginBottom:10}}>{title}</div>
      <div style={{display:"flex",flexDirection:"column",gap:6}}>
        {rows.map((r,i)=>(
          <div key={i} style={{display:"flex",alignItems:"center",gap:12,padding:"6px 10px",borderRadius:8,background:r.hex,color:r.dark?"#F4ECDD":"#14110C",border:"1px solid rgba(0,0,0,0.05)"}}>
            <span style={{fontSize:13,fontWeight:700,flex:1}}>{r.n}</span>
            <span className="num" style={{fontSize:11,opacity:.85,fontWeight:600}}>{r.hex}</span>
            <span className="num" style={{fontSize:10,opacity:.6}}>{r.v}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
function SemCard({name, desc, hex}) {
  return (
    <div className="card-machined" style={{padding:14}}>
      <div style={{height:60,borderRadius:10,background:hex,marginBottom:10,boxShadow:"inset 0 1px 0 rgba(255,255,255,0.1)"}}/>
      <div style={{fontSize:13,fontWeight:700}}>{name}</div>
      <div style={{fontSize:11,color:"var(--muted)"}}>{desc}</div>
      <div className="num" style={{fontSize:11,marginTop:6,color:"var(--graphite)"}}>{hex}</div>
    </div>
  );
}
function TypeRow({s,w,t,caps,children}) {
  return (
    <div style={{display:"flex",alignItems:"baseline",justifyContent:"space-between",padding:"6px 0",gap:14}}>
      <span style={{fontSize:s,fontWeight:w,letterSpacing:(t/100)+"em",textTransform:caps?"uppercase":"none",lineHeight:1.05,flex:1}}>{children}</span>
      <span style={{fontFamily:"var(--mono)",fontSize:10.5,letterSpacing:".12em",color:"var(--muted)"}}>HANKEN · {s}/{w} · {t>=0?"+":""}{t}%</span>
    </div>
  );
}
function StateCard({t,children}) {
  return (
    <div className="card-machined" style={{padding:14,display:"flex",flexDirection:"column"}}>
      <div className="eyebrow">{t}</div>
      {children}
    </div>
  );
}

Object.assign(window, { DesignSystem });
