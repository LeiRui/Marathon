//package SA;
//
//import HModel.Column_ian;
//import HModel.H_ian;
//import common.Constant;
//import queries.QueryPicture;
//import queries.RangeQuery;
//import replicas.AckSeq;
//import replicas.XAckSeq;
//
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.PrintWriter;
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.util.*;
//
//@Deprecated
//public class Unify{
//    public int margin_us = 0; // 查询路由采取cost最小原则 模糊限margin 单位us
//
//    public  boolean isDiffReplicated = false; // 统一无异构优化数据存储结构和异构
//
//    // 数据分布参数
//    private BigDecimal totalRowNumber;
//    private int ckn;
//    private List<Column_ian> CKdist;
//    // 数据存储参数
//    private int rowSize;// unit: byte
//    private int fetchRowCnt; // 工程实践设置的分批一次取结果最大行数
//    private double costModel_k; // merged cost: f(x)=k*x+b
//    private double costModel_b; // merged cost: f(x)=k*x+b
//    private double cost_session_around; // Cost的其余组成部分  unit:us
//    private double cost_request_around; // Cost的其余组成部分  unit:us
//
//    // 查询参数
//    QueryPicture queryPicture; // 描述参数
//    public RangeQuery[][] rangeQueries; // 符合描述分布的确定一组查询集合
//    int[][][] qChooseX; // 记录单查询计算过程中的路由结果 [qbatch][ckn][qper]
//    //public List<String> sqls;// 符合描述分布的确定一组查询集合
//    List<PrintWriter> pws; // X个副本分流的sqls最终结果记录
//
//    // SA
//    // X个异构副本组成一个状态解
//    // 分流策略：简单的代价最小原则
//    public BigDecimal Cost; // 某个状态解的代价评价：查询按照分流策略分流之后累计的查询代价
//    //public List<List<Integer>> qchooseX; // queries中每一个查询的路由结果记录，这里是从0开始
//    public BigDecimal Cost_best;// SA每次内圈得到新状态之后维护的最优状态值
//    public BigDecimal Cost_best_bigloop; // SA外圈循环记录每次外圈退温时记忆中保持的最优状态值
//    public Set<XAckSeq> ackSeq_best_step;//记忆性 SA维护的最优状态解集
//
////    public XAckSeq Output; // 最后的输出
//
//    public int X; // 给定的异构副本数量
//
//    public Unify(BigDecimal totalRowNumber, int ckn, List<Column_ian>CKdist,
//                              int rowSize, int fetchRowCnt, double costModel_k, double costModel_b, double cost_session_around, double cost_request_around,
//                              QueryPicture queryPicture,
//                              int X) {
//        this.totalRowNumber = totalRowNumber;
//        this.ckn = ckn;
//        this.CKdist = CKdist;
//
//        this.rowSize = rowSize;
//        this.fetchRowCnt = fetchRowCnt;
//        this.costModel_k = costModel_k;
//        this.costModel_b = costModel_b;
//        this.cost_session_around = cost_session_around;
//        this.cost_request_around = cost_request_around;
//
//        this.queryPicture = queryPicture;
////        sqls = new ArrayList();
//        rangeQueries = queryPicture.getDefinite(CKdist);
//        qChooseX = new int[queryPicture.totalQueryBatchNum][][];
//        for(int i=0;i<queryPicture.totalQueryBatchNum;i++) {
//            qChooseX[i]=new int[ckn][];
//            for(int j=0;j<ckn;j++) {
//                qChooseX[i][j] = new int[queryPicture.qpernum[j]];
//            }
//        }
//
//        this.ackSeq_best_step = new HashSet();
//        this.X = X;
//        //this.qchooseX = new ArrayList();
//        pws = new ArrayList<>();
//
//    }
//
//    /**
//     * 关键的状态评价函数
//     *
//     * 输入：一个状态,X个异构副本组成一个状态解
//     * 分流策略：最小Cost
//     * 状态目标函数值：TotalCost = max(sum Cost)
//     * 输出：输入状态的目标函数值TotalCost
//     *
//     * @param xackSeq X个异构副本组成一个状态解
//     */
//    public void calculate(AckSeq[] xackSeq) {
//        BigDecimal[] XCostload = new BigDecimal[X]; // 用于后面取max(sum HB)
//        for(int i=0;i<X;i++) {
//            XCostload[i] = new BigDecimal("0");
//        }
//
//        //qchooseX.clear(); // 每次要清空重新add
//        int qbatch = queryPicture.totalQueryBatchNum;
//        for(int i=0; i<qbatch; i++) {// 遍历所有qbatch
//            for(int k=0;k<ckn;k++) { // 遍历一个batch内所有列范围查询 TODO 增量式可以在这里缩小范围
//                RangeQuery q = rangeQueries[i][k];
//                int qper = queryPicture.qpernum[k];
//
//                BigDecimal[] recordCost = new BigDecimal[X]; // 单查询在X个副本上的代价
//                List<Integer> chooseX = new ArrayList(); // 记录路由结果，代价一样的副本平分负载
//                chooseX.add(0);
//                H_ian h = new H_ian(totalRowNumber, ckn, CKdist,
//                        q.qckn, q.qck_r1_abs, q.qck_r2_abs, q.r1_closed, q.r2_closed, q.qck_p_abs,
//                        xackSeq[0].ackSeq);
//                BigDecimal chooseCost = h.calculate(fetchRowCnt, costModel_k, costModel_b, cost_session_around, cost_request_around);
//                recordCost[0] = chooseCost;
//                BigDecimal tmpCost;
//
//                for (int j = 1; j < X; j++) { // 遍历X个副本，按照最小HB原则对q分流
//                    h = new H_ian(totalRowNumber, ckn, CKdist,
//                            q.qckn, q.qck_r1_abs, q.qck_r2_abs, q.r1_closed, q.r2_closed, q.qck_p_abs,
//                            xackSeq[j].ackSeq);
//                    tmpCost = h.calculate(fetchRowCnt, costModel_k, costModel_b, cost_session_around, cost_request_around);
//                    recordCost[j] = tmpCost;
//                    int res = tmpCost.compareTo(chooseCost);
//                    if (res == -1) {
//                        chooseCost = tmpCost; // note 引用
//                        chooseX.clear();
//                        chooseX.add(j);
//                    } else if (res == 0) {
//                        chooseX.add(j);
//                    }
//                }//X个副本遍历结束，现在已经确定了这个query按照精确最小Cost原则分流到的副本chooseX，以及这个最小Cost等于多少
//
//                BigDecimal margin = new BigDecimal(margin_us);//1ms
//                List<BigDecimal> chooseCostList = new ArrayList<BigDecimal>();
//                for (int c = 0; c < chooseX.size(); c++) {
//                    chooseCostList.add(chooseCost);
//                }
//                for (int j = 0; j < X; j++) { //重新遍历一遍，对分流模糊一层
//                    if (chooseX.contains(j)) {
//                        continue;
//                    }
//                    if (recordCost[j].subtract(chooseCost).compareTo(margin) == -1) {
//                        chooseX.add(j);
//                        chooseCostList.add(recordCost[j]);
//                    }
//                }
//                //qchooseX.add(chooseX);
//                //接下来更新XBload和XRload
//                int chooseNumber = chooseX.size();
//                Random random = new Random();
//                int start = random.nextInt(chooseNumber);
//                for (int j = 0; j < qper; j++) { // 一个batch内执行qper次这个单查询
//                    //现在随机在chooseX中选一个 //随机不太好 还是固定策略均分
//                    // TODO 不行 这个随机太不均了
//                    // 这里算法认为均匀分 实现时真的均分很重要
//                    int replica = chooseX.get(start);
////                    System.out.println(replica);
//                    qChooseX[i][k][j] = replica;
//                    XCostload[replica] = XCostload[replica].add(chooseCostList.get(start));
//                    start++;
//                    if(start>=chooseNumber) {
//                        start = 0;
//                    }
////                    int r = random.nextInt(chooseNumber);
////                    int replica = chooseX.get(random.nextInt(chooseNumber));
////                    System.out.println(replica);
//////                    System.out.println(""+i+" "+k+" "+j+" "+r);
//////                    System.out.println(chooseNumber);
//////                    System.out.println(chooseCostList.size());
////                    qChooseX[i][k][j] = replica;
////                    XCostload[replica] = XCostload[replica].add(chooseCostList.get(r));
//                }
//            }
//        }
//
//        // X个结点副本cost load中的最大值
//        if(isDiffReplicated) {// X个结点副本cost load中的最大值
//            List<Integer> maxC = new ArrayList<Integer>();
//            Cost = XCostload[0];
//            maxC.add(0);
//            for (int i = 1; i < X; i++) {
//                int res = XCostload[i].compareTo(Cost);
//                if (res == 1) {
//                    maxC.clear();
//                    maxC.add(i);
//                    Cost = XCostload[i];
//                } else if (res == 0) {
//                    maxC.add(i);
//                }
//            }
//        }
//        else { // 同构时为了减小算法中随机均分还是有的不均对结果的干扰，采用求和平均，重要，因为之前代价相等副本随机均分时的不均取max还是有很大影响
//            Cost = new BigDecimal("0");
//            for(int i=0;i<XCostload.length;i++){
//                Cost=Cost.add(XCostload[i]);
//            }
//            Cost=Cost.divide(new BigDecimal(X),10, RoundingMode.HALF_UP);
//        }
//
//        //打印结果
//        System.out.print(String.format("Cost:%.2f s| ",Cost));
//        for(int i=0;i<X;i++) {
//            System.out.print(String.format("R%d%s:%.2f s,",i+1,xackSeq[i],XCostload[i])); // 用查询执行耗时作为load负载评价
//        }
////        for(int i=0;i<qnum; i++) {
////            System.out.print(String.format("|q%d->",i+1));
////            List<Integer> chooseX = qchooseX.get(i);
////            for(int j=0;j<chooseX.size();j++) {
////                System.out.printf("R%d",chooseX.get(j)+1);
////                if(j!=chooseX.size()-1) {
////                    System.out.print(",");
////                }
////            }
////        }
//        System.out.println("");
//    }
//
//    /*
//     直接给定一个排序，给出结果包括查询集合
//     */
//    public void calculate_unit(AckSeq[] xackSeq) {
//        BigDecimal[] XCostload = new BigDecimal[X]; // 用于后面取max(sum HB)
//        for(int i=0;i<X;i++) {
//            XCostload[i] = new BigDecimal("0");
//        }
//
//        int qbatch = queryPicture.totalQueryBatchNum;
//        for(int i=0; i<qbatch; i++) {// 遍历所有qbatch
//            for(int k=0;k<ckn;k++) { // 遍历一个batch内所有列范围查询 TODO 增量式可以在这里缩小范围
//                RangeQuery q = rangeQueries[i][k];
//                int qper = queryPicture.qpernum[k];
//
//                BigDecimal[] recordCost = new BigDecimal[X]; // 单查询在X个副本上的代价
//                List<Integer> chooseX = new ArrayList(); // 记录路由结果，代价一样的副本平分负载
//                chooseX.add(0);
//                H_ian h = new H_ian(totalRowNumber, ckn, CKdist,
//                        q.qckn, q.qck_r1_abs, q.qck_r2_abs, q.r1_closed, q.r2_closed, q.qck_p_abs,
//                        xackSeq[0].ackSeq);
//                BigDecimal chooseCost = h.calculate(fetchRowCnt, costModel_k, costModel_b, cost_session_around, cost_request_around);
//                recordCost[0] = chooseCost;
//                BigDecimal tmpCost;
//
//                for (int j = 1; j < X; j++) { // 遍历X个副本，按照最小HB原则对q分流
//                    h = new H_ian(totalRowNumber, ckn, CKdist,
//                            q.qckn, q.qck_r1_abs, q.qck_r2_abs, q.r1_closed, q.r2_closed, q.qck_p_abs,
//                            xackSeq[j].ackSeq);
//                    tmpCost = h.calculate(fetchRowCnt, costModel_k, costModel_b, cost_session_around, cost_request_around);
//                    recordCost[j] = tmpCost;
//                    int res = tmpCost.compareTo(chooseCost);
//                    if (res == -1) {
//                        chooseCost = tmpCost; // note 引用
//                        chooseX.clear();
//                        chooseX.add(j);
//                    } else if (res == 0) {
//                        chooseX.add(j);
//                    }
//                }//X个副本遍历结束，现在已经确定了这个query按照精确最小Cost原则分流到的副本chooseX，以及这个最小Cost等于多少
//
//                BigDecimal margin = new BigDecimal(margin_us);//1ms
//                List<BigDecimal> chooseCostList = new ArrayList<BigDecimal>();
//                for (int c = 0; c < chooseX.size(); c++) {
//                    chooseCostList.add(chooseCost);
//                }
//                for (int j = 0; j < X; j++) { //重新遍历一遍，对分流模糊一层
//                    if (chooseX.contains(j)) {
//                        continue;
//                    }
//                    if (recordCost[j].subtract(chooseCost).compareTo(margin) == -1) {
//                        chooseX.add(j);
//                        chooseCostList.add(recordCost[j]);
//                    }
//                }
//                //接下来更新XBload和XRload
//                int chooseNumber = chooseX.size();
//                Random random = new Random();
//                int start = random.nextInt(chooseNumber);
//                for (int j = 0; j < qper; j++) { // 一个batch内执行qper次这个单查询
//                    //现在随机在chooseX中选一个 //随机不太好 还是固定策略均分
//                    // TODO 不行 这个随机太不均了
//                    // 这里算法认为均匀分 实现时真的均分很重要
//                    int replica = chooseX.get(start);
//                    qChooseX[i][k][j] = replica;
//                    XCostload[replica] = XCostload[replica].add(chooseCostList.get(start));
//                    start++;
//                    if(start>=chooseNumber) {
//                        start = 0;
//                    }
//                }
//            }
//        }
//
//        // X个结点副本cost load中的最大值
//        if(isDiffReplicated) {// X个结点副本cost load中的最大值
//            List<Integer> maxC = new ArrayList<Integer>();
//            Cost = XCostload[0];
//            maxC.add(0);
//            for (int i = 1; i < X; i++) {
//                int res = XCostload[i].compareTo(Cost);
//                if (res == 1) {
//                    maxC.clear();
//                    maxC.add(i);
//                    Cost = XCostload[i];
//                } else if (res == 0) {
//                    maxC.add(i);
//                }
//            }
//        }
//        else { // 同构时为了减小算法中随机均分还是有的不均对结果的干扰，采用求和平均，重要，因为之前代价相等副本随机均分时的不均取max还是有很大影响
//            Cost = new BigDecimal("0");
//            for(int i=0;i<XCostload.length;i++){
//                Cost=Cost.add(XCostload[i]);
//            }
//            Cost=Cost.divide(new BigDecimal(X),10, RoundingMode.HALF_UP);
//        }
//
//        //打印结果
//        System.out.print(String.format("Cost:%.2f s| ",Cost));
//        for(int i=0;i<X;i++) {
//            System.out.print(String.format("R%d%s:%.2f s,",i+1,xackSeq[i],XCostload[i])); // 用查询执行耗时作为load负载评价
//        }
//        System.out.println("");
//
//        pws=new ArrayList<>();
//        for(int i=0;i<X;i++) {
//            try {
//                PrintWriter pw_ = new PrintWriter(new FileOutputStream(Constant.ks+"_"+Constant.cf[i]
//                        +"_R" + (i + 1)+ xackSeq[i]+String.format("_%.2f",Cost)+ "_sqls.txt"));
//                pws.add(pw_);
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//        }
//        for(int i=0;i<queryPicture.totalQueryBatchNum;i++){
//            for(int k=0;k<ckn;k++){
//                for(int j=0;j<queryPicture.qpernum[k];j++){
//                    int direct = qChooseX[i][k][j];
//                    pws.get(direct).println(queryPicture.getSql(Constant.ks, Constant.cf[direct],Constant.pkey,
//                            CKdist,rangeQueries[i][k]));
//                }
//            }
//        }
//        for(PrintWriter pw_: pws) {
//            pw_.close();
//        }
//        if(!isDiffReplicated){ // 如果同构 再写一个集合全部查询的大文件
//            PrintWriter pw_=null;
//            try {
//                pw_ = new PrintWriter(new FileOutputStream(Constant.ks+"_"+Constant.cf[0]
//                        +"_R" + xackSeq[0]+String.format("_%.2f",Cost)+ "_sqlsALL.txt"));
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//            for(int i=0;i<queryPicture.totalQueryBatchNum;i++){
//                for(int k=0;k<ckn;k++){
//                    for(int j=0;j<queryPicture.qpernum[k];j++){
//                        pw_.println(queryPicture.getSql(Constant.ks, Constant.cf[0],Constant.pkey,
//                                CKdist,rangeQueries[i][k]));
//                    }
//                }
//            }
//            pw_.close();
//        }
//    }
//
////    /**
////     * 关键的状态评价函数  打印结果到文件版本
////     *
////     * 输入：一个状态,X个异构副本组成一个状态解
////     * 分流策略：最小Cost  TODO 暂时用精确Cost最小作为分流策略
////     * 状态目标函数值：TotalCost = max(sum Cost)
////     * 输出：输入状态的目标函数值TotalCost
////     *
////     * @param xackSeq X个异构副本组成一个状态解
////     */
////    public void calculate(AckSeq[] xackSeq, PrintWriter pw) {
////        BigDecimal[] XCostload = new BigDecimal[X]; // 用于后面取max(sum HB)
////        for(int i=0;i<X;i++) {
////            XCostload[i] = new BigDecimal("0");
////        }
////
////        qchooseX.clear(); // 每次要清空重新add
////        int qnum = queries.size();
////        for(int i=0; i<qnum; i++) {// 遍历queries
////            RangeQuery q = queries.get(i);
////            int qper = queriesPerc.get(i);
////
////            BigDecimal[] recordCost = new BigDecimal[X];
////            List<Integer> chooseX = new ArrayList(); // 代价一样的副本平分负载
////            chooseX.add(0);
////            H_ian h = new H_ian(totalRowNumber,ckn,CKdist,
////                    q.qckn,q.qck_r1_abs,q.qck_r2_abs,q.r1_closed,q.r2_closed, q.qck_p_abs,
////                    xackSeq[0].ackSeq);
////            BigDecimal chooseCost = h.calculate(fetchRowCnt,costModel_k,costModel_b,cost_session_around,cost_request_around);
////            recordCost[0] = chooseCost;
////            BigDecimal tmpCost;
////            if(sqls.size() == i) {
////                sqls.add(h.getSql(Constant.ks,Constant.cf));
////            }
////            for(int j=1;j<X;j++) { // 遍历X个副本，按照最小HB原则对q分流
////                h = new H_ian(totalRowNumber,ckn,CKdist,
////                        q.qckn,q.qck_r1_abs,q.qck_r2_abs,q.r1_closed,q.r2_closed, q.qck_p_abs,
////                        xackSeq[j].ackSeq);
////                tmpCost = h.calculate(fetchRowCnt,costModel_k,costModel_b,cost_session_around,cost_request_around);
////                recordCost[j]=tmpCost;
////                int res = tmpCost.compareTo(chooseCost);
////                if(res == -1) {
////                    chooseCost = tmpCost; // note 引用
////                    chooseX.clear();
////                    chooseX.add(j);
////                }
////                else if(res == 0) { // TODO 暂时用精确的话这个几乎不会发生 等到模糊化时再探讨
////                    chooseX.add(j);
////                }
////            }//X个副本遍历结束，现在已经确定了这个query按照最小HB原则分流到的副本chooseX，以及这个最小HB等于多少
////
////            int margin_us = 0;
////            BigDecimal margin = new BigDecimal(margin_us);//1ms
////            List<BigDecimal> chooseCostList = new ArrayList<BigDecimal>();
////            for(int c=0;c<chooseX.size();c++) {
////                chooseCostList.add(chooseCost);
////            }
////            for(int j=0;j<X;j++) { //重新遍历一遍，对分流模糊一层
////                if(chooseX.contains(j)) {
////                    continue;
////                }
////                if(recordCost[j].subtract(chooseCost).compareTo(margin)==-1){
////                    chooseX.add(j);
////                    chooseCostList.add(recordCost[j]);
////                }
////            }
////
////            qchooseX.add(chooseX);
////            //接下来更新XBload和XRload
////            int chooseNumber = chooseX.size();
////            BigDecimal averageQPer = new BigDecimal(qper).divide(new BigDecimal(chooseNumber),10, RoundingMode.HALF_UP);
////            //BigDecimal averageHB = chooseCost.multiply(averageQPer);
////            for(int j=0;j<chooseNumber;j++) {
////                int choose = chooseX.get(j);
////                XCostload[choose]=XCostload[choose].add(chooseCostList.get(j).multiply(averageQPer)); // note 光是.add是不行的 要赋值！
////            }
////        }
////
////        // max(sum HB)的max
////        List<Integer> maxR=new ArrayList<Integer>();
////        Cost = XCostload[0];
////        maxR.add(0);
////        for(int i=1;i<X;i++) {
////            int res = XCostload[i].compareTo(Cost);
////            if(res == 1) {
////                maxR.clear();
////                maxR.add(i);
////                Cost = XCostload[i];
////            }
////            else if(res == 0) {
////                maxR.add(i);
////            }
////        }
////
////        //打印结果
//////        System.out.print(String.format("Cost:%.2f us| ",Cost));
////        pw.write(String.format("%.2f,,",Cost));
////        double sumLoad = 0;
////        for(int i=0;i<X;i++) {
//////            System.out.print(String.format("ck%d%s:%.2f us,",i+1,xackSeq[i],XCostload[i])); // 用查询执行耗时作为load负载评价
////            pw.write(String.format("R%d%s,%.2f,",i+1,xackSeq[i],XCostload[i])); // 用查询执行耗时作为load负载评价
////            sumLoad += XCostload[i].doubleValue();
////        }
////        // 计算load标准差 标准差能反映一个数据集的离散程度
////        double averageLoad = sumLoad/X;
////        double tmp = 0;
////        for(int i=0;i<X;i++) {
////            tmp += (XCostload[i].doubleValue()-averageLoad)*(XCostload[i].doubleValue()-averageLoad);
////        }
////        tmp = Math.sqrt(tmp/X);
////        pw.write(tmp+",");
////
////        pw.write(",");
////        for(int i=0;i<qnum; i++) {
//////            System.out.print(String.format("|q%d->",i+1));
////            List<Integer> chooseX = qchooseX.get(i);
////            for(int j=0;j<chooseX.size();j++) {
//////                System.out.printf("R%d",chooseX.get(j)+1);
////                pw.write(String.format("R%d",chooseX.get(j)+1));
////                if(j!=chooseX.size()-1) {
////                    //System.out.print(",");
////                    pw.write("|");
////                }
////            }
////            if(i!=qnum-1) {
////                pw.write(",");
////            }
////            else {
////                pw.write("\n");
////            }
////        }
////    }
//
//
//
//    /**
//     * 模拟退火算法
//     * 搜索排序键排列，找到使得在给定数据和查询集的条件下，H模型代价最小（已经验证H模型代价和真实查询代价的一致性）
//     * 关键环节：
//     * 1. 初温，初始解
//     * 2. 状态产生函数
//     * 3. 状态接受函数
//     * 4. 退温函数
//     * 5. 抽样稳定准则
//     * 6. 收敛准则
//     *
//     * X个异构副本组成一个状态解
//     * 分流策略：简单的代价最小原则
//     *
//     * SA一步式尚未考虑负载均衡代价：找到HR近似最小的一组解
//     * 状态目标值为HR
//     *
//     */
//    public void SA_b() {
//        //确定初温：
//        // 随机产生一组状态，确定两两状态间的最大目标值差，然后依据差值，利用一定的函数确定初温
//        int setNum = 20;
//        List<AckSeq[]> xackSeqList = new ArrayList();
//        for(int i=0; i< setNum; i++) {
//            AckSeq[] xackSeq = new AckSeq[X];
//            shuffle(xackSeq);
//            xackSeqList.add(xackSeq);
//        }
//        BigDecimal maxDeltaC = new BigDecimal("0"); // 两两状态间的最大目标值差
//        for(int i=0;i<setNum-1;i++) {
//            for(int j =i+1;j<setNum;j++) {
//                calculate(xackSeqList.get(i));
//                BigDecimal tmp = new BigDecimal(Cost.toString());
//                calculate(xackSeqList.get(j));
//                tmp = tmp.subtract(Cost).abs();
//                if(tmp.compareTo(maxDeltaC) == 1) // tmp > maxDeltaB
//                    maxDeltaC = tmp;
//            }
//        }
//        double pr = 0.8;
//        if(maxDeltaC.compareTo(new BigDecimal("0")) == 0) {
//            maxDeltaC = new BigDecimal("0.001");
//        }
//        BigDecimal t0 = maxDeltaC.negate().divide(new BigDecimal(Math.log(pr)),10, RoundingMode.HALF_UP);
//        System.out.println("初温为："+t0);
//
//        //确定初始解
//        AckSeq[] currentAckSeq  = new AckSeq[X];
//        shuffle(currentAckSeq);
//        calculate(currentAckSeq);
//        Cost_best = new BigDecimal(Cost.toString()); // 至于把currentAckSeq加进Set在后面完成的
//        Cost_best_bigloop = new BigDecimal(Cost.toString());
//
//        int endCriteria = 30;// 终止准则: BEST SO FAR连续20次退温保持不变
//        int endCount = 0;
//        int sampleCount = 20;// 抽样稳定准则：20步定步长
//        BigDecimal deTemperature = new BigDecimal("0.7"); // 指数退温系数
//        while(endCount < endCriteria) {
//            // 抽样稳定
//            // 记忆性：注意中间最优结果记下来
//            for(int sampleloop = 0; sampleloop < sampleCount; sampleloop++) {
//                //增加记忆性
//                int comp = Cost.compareTo(Cost_best);
//                if(comp==-1) { //<
//                    Cost_best = Cost;
//                    ackSeq_best_step.clear();
//                    ackSeq_best_step.add(new XAckSeq(currentAckSeq));
//                }
//                else if(comp==0) {
//                    ackSeq_best_step.add(new XAckSeq(currentAckSeq));
//                }
//
//                //由当前状态产生新状态
//                AckSeq[] nextAckSeq = generateNewStateX(currentAckSeq);
//                //接受函数接受否
//                BigDecimal currentCost = new BigDecimal(Cost.toString()); // 当前状态的状态值保存在Cost
//                calculate(nextAckSeq); // Cost会被改变
//                BigDecimal delta = Cost.subtract(currentCost); // 新旧状态的目标函数值差
//                double threshold;
//                if(delta.compareTo(new BigDecimal("0"))!=1) { // <
//                    threshold = 1;
//                    System.out.println("新状态不比现在状态差");
//                }
//                else {
//                    //threshold = Math.exp(-delta/t0);
//                    threshold = Math.exp(delta.negate().divide(t0,10, RoundingMode.HALF_UP).doubleValue()); //TODO
//                    System.out.println("新状态比现在状态差");
//                }
//
//                if(Math.random() <= threshold) { // 概率接受，替换当前状态
//                    currentAckSeq = nextAckSeq;
//                    System.out.println("接受新状态");
//                    // HR就是现在更新后的HR
//                }
//                else {// 否则保持当前状态不变
//                    Cost = currentCost;//恢复原来解的状态值
//                    System.out.println("维持当前状态不变");
//                    // currentAckSeq就是原本的
//                }
//            }
//
//            if(!Cost_best.equals(Cost_best_bigloop)) {
//                endCount = 0; // 重新计数
//                Cost_best_bigloop = Cost_best; // 把当前最小值传递给外圈循环
//                System.out.println("【这次退温BEST SO FAR改变】");
//            }
//            else { // 这次退温后best_so_far和上次比没有改变
//                endCount++;
//                System.out.println(String.format("【这次退温BEST SO FAR连续%d次没有改变】",endCount));
//            }
//
//            //退温
//            t0 = t0.multiply(deTemperature);
//        }
//        //终止 输出结果
//    }
//
//
//    public void combine() {
////        PrintWriter pw = null;
////        try {
////            pw = new PrintWriter(new FileOutputStream("unify_Mara_obscure_res1.csv"));
////        } catch (FileNotFoundException e) {
////            e.printStackTrace();
////        }
////        pw.write("Cost,,");
////        for(int i=0;i<X;i++) {
////            pw.write("R"+(i+1)+"_ackSeq,");
////            pw.write("R"+(i+1)+"_loadCost,");
////        }
////        pw.write("stdev.p\n");
//
////        int loop=1;
////        for(int id = 0;id<loop; id++) {
//        System.out.println("----------------------------------------------------");
//        // 第一步： SA找到Cost代价近似最小的一组解
//        SA_b();
//        System.out.println("step完成: SA找到Cost近似最小的" + ackSeq_best_step.size() + "个近似最优解:");
////            for (XAckSeq xackSeq : ackSeq_best_step) {
////                calculate(xackSeq.xackSeq); // 注意每次calculate在chooseX时有一定随机性，以最后留下的版本为准
////                //calculate(xackSeq.xackSeq, pw);
////            } // 此时结束之后Output以及unify中的所有属性都是ackSeq_best_step中最后一个元素的计算结果
//////            System.out.println("目标值Cost近似最小为：" + Cost_best + "(us)");
//        System.out.println("选择最后一个解作为输出");
//        Iterator iterator = ackSeq_best_step.iterator();
//        XAckSeq xackSeq = (XAckSeq) iterator.next();
//        while (iterator.hasNext()) {
//            xackSeq = (XAckSeq) iterator.next();
//        }
//        calculate(xackSeq.xackSeq);
//        System.out.println(String.format("目标值Cost近似最小为：%.2f (s)", Cost));
//
////        pw.write("\n");
////        }
////        pw.close();
//
//
//        //记录最后一个最优解的查询路由
//        pws = new ArrayList<>();
//        for(int i=0;i<X;i++) {
//            try {
//                PrintWriter pw_ = new PrintWriter(new FileOutputStream(Constant.ks+"_"+Constant.cf[i]
//                        +"_R" + (i + 1)+ xackSeq.xackSeq[i]+String.format("_%.2f",Cost)+ "_sqls.txt"));
//                pws.add(pw_);
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//        }
//        for(int i=0;i<queryPicture.totalQueryBatchNum;i++){
//            for(int k=0;k<ckn;k++){
//                for(int j=0;j<queryPicture.qpernum[k];j++){
//                    int direct = qChooseX[i][k][j];
//                    pws.get(direct).println(queryPicture.getSql(Constant.ks, Constant.cf[direct],Constant.pkey,
//                            CKdist,rangeQueries[i][k]));
//                }
//            }
//        }
//        for(PrintWriter pw_: pws) {
//            pw_.close();
//        }
//
//        if(!isDiffReplicated){ // 如果同构 再写一个集合全部查询的大文件
//            PrintWriter pw_=null;
//            try {
//                pw_ = new PrintWriter(new FileOutputStream(Constant.ks+"_"+Constant.cf[0]
//                        +"_R" + xackSeq.xackSeq[0]+String.format("_%.2f",Cost)+ "_sqlsALL.txt"));
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//            for(int i=0;i<queryPicture.totalQueryBatchNum;i++){
//                for(int k=0;k<ckn;k++){
//                    for(int j=0;j<queryPicture.qpernum[k];j++){
//                        pw_.println(queryPicture.getSql(Constant.ks, Constant.cf[0],Constant.pkey,
//                                CKdist,rangeQueries[i][k]));
//                    }
//                }
//            }
//            pw_.close();
//        }
//
//
//
////        for(int i=0;i<sqls.size(); i++) {
////            System.out.println(sqls.get(i)+":"+queriesPerc.get(i));
////        }
//
//    }
//
//    private void shuffle(AckSeq[] xackSeq) {
//        if(isDiffReplicated) {// 副本异构
//            for (int j = 0; j < X; j++) {
//                List<Integer> ackList = new ArrayList();
//                xackSeq[j] = new AckSeq(new int[ckn]);
//                for (int i = 1; i <= ckn; i++) { // 这里必须从1开始，因为表示ck排序输入参数从1开始
//                    ackList.add(i);
//                }
//                Collections.shuffle(ackList); //JAVA的Collections类中shuffle的用法
//                for (int i = 0; i < ckn; i++) {
//                    xackSeq[j].ackSeq[i] = ackList.get(i);
//                }
//            }
//        }
//        else { // 无异构
//            List<Integer> ackList = new ArrayList();
//            for (int i = 1; i <= ckn; i++) { // 这里必须从1开始，因为表示ck排序输入参数从1开始
//                ackList.add(i);
//            }
//            Collections.shuffle(ackList); //JAVA的Collections类中shuffle的用法
//            for (int j = 0; j < X; j++) {
//                xackSeq[j] = new AckSeq(new int[ckn]);
//                for (int i = 0; i < ckn; i++) {
//                    xackSeq[j].ackSeq[i] = ackList.get(i);
//                }
//            }
//        }
//    }
//
//
//    private AckSeq[] generateNewStateX(AckSeq[] xackSeq) {
//        AckSeq[] nextXAckSeq = new AckSeq[X];
//        if(isDiffReplicated) { // 副本异构
//            for (int i = 0; i < X; i++) {
//                nextXAckSeq[i] = new AckSeq(generateNewState(xackSeq[i].ackSeq));
//            }
//        }
//        else { // 无异构
//            int[] tmp = generateNewState(xackSeq[0].ackSeq);
//            for (int i = 0; i < X; i++) {
//                nextXAckSeq[i] = new AckSeq(tmp);
//            }
//        }
//        return nextXAckSeq;
//    }
//
//    /**
//     * 状态产生函数/邻域函数：由产生候选解的方式和候选解产生的概率分布两部分组成。
//     * 出发点：尽可能保证产生的候选解遍布全部解空间
//     *
//     * @param ackSeq
//     */
//    private int[] generateNewState(int[] ackSeq) {
//        int [] nextAckSeq = new int[ckn];
//        for(int i=0;i<ckn;i++) {
//            nextAckSeq[i] = ackSeq[i];
//        }
//
//        double p_swap = 0.3; // [0,0.3)
//        double p_inverse = 0.6; // [0.3,0.6)
//        double p_insert = 1; // [0.6,1)
//
//        double r = Math.random();
//        if(r<p_swap) {
//            generateNewState_swap(nextAckSeq);
//        }
//        else if(r<p_inverse) {
//            generateNewState_inverse(nextAckSeq);
//        }
//        else {
//            generateNewState_insert(nextAckSeq);
//        }
//        return  nextAckSeq;
//    }
//
//    //随机交换状态中两个不同位置的ck
//    private void generateNewState_swap(int[] ackSeq){
//        Random random = new Random();
//        int pos1 = random.nextInt(ckn); // 0~ckn-1
//        int pos2 = random.nextInt(ckn);
//        while(true) {
//            if(pos2!=pos1)
//                break;
//            else {
//                pos2 = random.nextInt(ckn);
//            }
//        }
//        int tmp = ackSeq[pos1];
//        ackSeq[pos1] = ackSeq[pos2];
//        ackSeq[pos2] = tmp;
//    }
//
//    //将两个不同位置间的串逆序
//    private void generateNewState_inverse(int[] ackSeq) {
//        Random random = new Random();
//        int pos1 = random.nextInt(ckn); // 0~ckn-1
//        int pos2 = random.nextInt(ckn);
//        while(true) {
//            if(pos2!=pos1)
//                break;
//            else {
//                pos2 = random.nextInt(ckn);
//            }
//        }
//        if(pos1>pos2) { // make pos1<pos2
//            int tmp = pos1;
//            pos1 = pos2;
//            pos2 = tmp;
//        }
//        int[] backup = new int[pos2-pos1+1];
//        for(int i=pos1; i<= pos2; i++) {
//            backup[i-pos1] = ackSeq[i];
//        }
//        for(int i=pos1; i<=pos2; i++) {
//            ackSeq[i] = backup[pos2-i];
//        }
//    }
//
//    //随机选择某一位置的ck并插入到另一随机位置
//    private void generateNewState_insert(int[] ackSeq) {
//        Random random = new Random();
//        int pos1 = random.nextInt(ckn); // 0~ckn-1
//        int pos2 = random.nextInt(ckn);
//        while(true) {
//            if(pos2!=pos1)
//                break;
//            else {
//                pos2 = random.nextInt(ckn);
//            }
//        }
//        //把pos1位置的ck插入到pos2位置
//        int[] add = new int[ckn+1];
//        for(int i=0;i<ckn+1;i++) {
//            if(i==pos2) {
//                add[i] = ackSeq[pos1];
//            }
//            else if(i>pos2) {
//                add[i] = ackSeq[i-1];
//            }
//            else {
//                add[i] = ackSeq[i];
//            }
//        }
//        for(int i = 0; i<ckn; i++) {
//            if(pos1<pos2) {
//                if (i >= pos1) {
//                    ackSeq[i] = add[i + 1];
//                } else {
//                    ackSeq[i] = add[i];
//                }
//            }
//            else {
//                if(i <= pos1) {
//                    ackSeq[i] = add[i];
//                }
//                else {
//                    ackSeq[i] = add[i+1];
//                }
//            }
//        }
//    }
//}
//
